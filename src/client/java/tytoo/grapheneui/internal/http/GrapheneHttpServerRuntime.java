package tytoo.grapheneui.internal.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.GrapheneHttpConfig;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.internal.resource.GrapheneMimeTypes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class GrapheneHttpServerRuntime implements GrapheneHttpServer, AutoCloseable {
    private static final String PATH_DELIMITER = "/";
    private static final String ASSETS_PREFIX = "assets" + PATH_DELIMITER;
    private static final String ALLOW_METHODS = "GET, HEAD";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ALLOW = "Allow";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final GrapheneHttpServerRuntime DISABLED = new GrapheneHttpServerRuntime("", -1, "", null, null, false);

    private final String host;
    private final int port;
    private final String baseUrl;
    private final HttpServer server;
    private final String spaFallbackResourcePath;
    private volatile boolean running;

    private GrapheneHttpServerRuntime(
            String host,
            int port,
            String baseUrl,
            HttpServer server,
            String spaFallbackResourcePath,
            boolean running
    ) {
        this.host = host;
        this.port = port;
        this.baseUrl = baseUrl;
        this.server = server;
        this.spaFallbackResourcePath = spaFallbackResourcePath;
        this.running = running;
    }

    public static GrapheneHttpServerRuntime disabled() {
        return DISABLED;
    }

    public static GrapheneHttpServerRuntime start(GrapheneHttpConfig config) {
        GrapheneHttpConfig validatedConfig = Objects.requireNonNull(config, "config");
        InetAddress bindAddress = resolveLoopbackAddress(validatedConfig.bindHost());
        HttpServer server = createServer(validatedConfig, bindAddress);
        String spaFallbackResourcePath = validatedConfig.spaFallback().map(ClasspathAssetHttpHandler::normalizeRequestPath).orElse(null);
        server.createContext(PATH_DELIMITER, new ClasspathAssetHttpHandler(spaFallbackResourcePath));
        server.start();

        int boundPort = server.getAddress().getPort();
        String baseUrl = buildBaseUrl(validatedConfig.baseUrlScheme(), bindAddress.getHostAddress(), boundPort);
        return new GrapheneHttpServerRuntime(bindAddress.getHostAddress(), boundPort, baseUrl, server, spaFallbackResourcePath, true);
    }

    private static InetAddress resolveLoopbackAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (!address.isLoopbackAddress()) {
                throw new IllegalArgumentException("Graphene HTTP bindHost must resolve to loopback: " + host);
            }

            return address;
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Failed to resolve Graphene HTTP bindHost: " + host, exception);
        }
    }

    private static HttpServer createServer(GrapheneHttpConfig config, InetAddress bindAddress) {
        if (config.fixedPort().isPresent()) {
            int port = config.fixedPort().orElseThrow();
            try {
                return HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to bind Graphene HTTP server on port " + port, exception);
            }
        }

        GrapheneHttpConfig.PortRange portRange = config.randomPortRange().orElseThrow();
        int minPort = portRange.minPort();
        int maxPort = portRange.maxPort();
        int candidateCount = maxPort - minPort + 1;
        int randomOffset = ThreadLocalRandom.current().nextInt(candidateCount);
        for (int attempt = 0; attempt < candidateCount; attempt++) {
            int candidatePort = minPort + Math.floorMod(randomOffset + attempt, candidateCount);
            try {
                return HttpServer.create(new InetSocketAddress(bindAddress, candidatePort), 0);
            } catch (IOException ignored) {
                // Try the next candidate port in range.
            }
        }

        throw new IllegalStateException("No available port in range " + minPort + "-" + maxPort + " for Graphene HTTP server");
    }

    private static String buildBaseUrl(String scheme, String host, int port) {
        try {
            return new URI(scheme, null, host, port, null, null, null).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Failed to build Graphene HTTP base URL", exception);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    public String spaFallbackResourcePath() {
        return spaFallbackResourcePath;
    }

    @Override
    public void close() {
        if (!running || server == null) {
            return;
        }

        running = false;
        server.stop(0);
    }

    private record ResourceResponse(int statusCode, String contentType, byte[] payload) {
        private ResourceResponse {
            payload = payload == null ? EMPTY_BYTES : payload;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (!(object instanceof ResourceResponse(
                    int otherStatusCode, String otherContentType, byte[] otherPayload
            ))) {
                return false;
            }

            return statusCode == otherStatusCode
                    && Objects.equals(contentType, otherContentType)
                    && Arrays.equals(payload, otherPayload);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(statusCode);
            result = 31 * result + Objects.hashCode(contentType);
            result = 31 * result + Arrays.hashCode(payload);
            return result;
        }

        @Override
        public @NonNull String toString() {
            return "ResourceResponse[statusCode="
                    + statusCode
                    + ", contentType="
                    + contentType
                    + ", payload="
                    + Arrays.toString(payload)
                    + "]";
        }
    }

    private record ClasspathAssetHttpHandler(String spaFallbackResourcePath) implements HttpHandler {
        private static InputStream openResource(String path) {
            ClassLoader classLoader = GrapheneHttpServerRuntime.class.getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(path);
            if (stream != null || path.startsWith(ASSETS_PREFIX)) {
                return stream;
            }

            return classLoader.getResourceAsStream(ASSETS_PREFIX + GrapheneCore.ID + PATH_DELIMITER + path);
        }

        private static String normalizeRequestPath(String requestPath) {
            String normalizedPath = requestPath == null ? "" : requestPath.trim();
            while (normalizedPath.startsWith(PATH_DELIMITER)) {
                normalizedPath = normalizedPath.substring(1);
            }

            return normalizedPath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange == null) {
                return;
            }

            try (exchange) {
                String method = exchange.getRequestMethod();
                boolean isHeadRequest = "HEAD".equalsIgnoreCase(method);
                boolean isGetRequest = "GET".equalsIgnoreCase(method);
                if (!isGetRequest && !isHeadRequest) {
                    Headers responseHeaders = exchange.getResponseHeaders();
                    responseHeaders.set(HEADER_ALLOW, ALLOW_METHODS);
                    responseHeaders.set(HEADER_CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN);
                    byte[] payload = "Method Not Allowed".getBytes(StandardCharsets.UTF_8);
                    send(exchange, 405, CONTENT_TYPE_TEXT_PLAIN, payload, false);
                    return;
                }

                String requestPath = normalizeRequestPath(exchange.getRequestURI().getPath());
                if (requestPath.contains("..")) {
                    send(exchange, 400, CONTENT_TYPE_TEXT_PLAIN, "Invalid path".getBytes(StandardCharsets.UTF_8), isHeadRequest);
                    return;
                }

                ResourceResponse response = loadResourceResponse(requestPath, isGetRequest);
                send(exchange, response.statusCode(), response.contentType(), response.payload(), isHeadRequest);
            }
        }

        private ResourceResponse loadResourceResponse(String requestPath, boolean isGetRequest) {
            ResourceResponse directResponse = loadResource(requestPath);
            if (directResponse.statusCode() == 200) {
                return directResponse;
            }

            if (!isGetRequest || spaFallbackResourcePath == null || spaFallbackResourcePath.isBlank()) {
                return directResponse;
            }

            if (requestPath.startsWith(ASSETS_PREFIX)) {
                return directResponse;
            }

            return loadResource(spaFallbackResourcePath);
        }

        private ResourceResponse loadResource(String requestPath) {
            String normalizedPath = normalizeRequestPath(requestPath);
            if (normalizedPath.isBlank()) {
                return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
            }

            try (InputStream inputStream = openResource(normalizedPath)) {
                if (inputStream == null) {
                    return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
                }

                byte[] payload = inputStream.readAllBytes();
                String contentType = GrapheneMimeTypes.resolve(normalizedPath);
                return new ResourceResponse(200, contentType, payload);
            } catch (IOException ignored) {
                // Failed to read classpath resource payload.
                return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
            }
        }

        private void send(HttpExchange exchange, int statusCode, String contentType, byte[] payload, boolean headRequest) throws IOException {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set(HEADER_CONTENT_TYPE, contentType == null ? CONTENT_TYPE_TEXT_PLAIN : contentType);

            byte[] responsePayload = payload == null ? EMPTY_BYTES : payload;
            if (headRequest) {
                exchange.sendResponseHeaders(statusCode, responsePayload.length);
                return;
            }

            exchange.sendResponseHeaders(statusCode, responsePayload.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responsePayload);
            }
        }
    }
}
