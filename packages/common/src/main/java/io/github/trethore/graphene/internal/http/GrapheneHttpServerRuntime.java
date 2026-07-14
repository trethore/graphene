package io.github.trethore.graphene.internal.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.trethore.graphene.api.config.GrapheneHttpConfig;
import io.github.trethore.graphene.api.runtime.GrapheneHttpServer;
import io.github.trethore.graphene.api.url.AssetId;
import io.github.trethore.graphene.internal.resource.GrapheneMimeTypes;
import io.github.trethore.graphene.internal.url.GrapheneHttpUrls;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class GrapheneHttpServerRuntime implements GrapheneHttpServer, AutoCloseable {
  private static final String PATH_DELIMITER = "/";
  private static final String ASSETS_PREFIX = "assets" + PATH_DELIMITER;
  private static final String MODS_PREFIX = "mods" + PATH_DELIMITER;
  private static final String ALLOW_METHODS = "GET, HEAD, POST";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  private static final String HEADER_ALLOW = "Allow";
  private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
  private static final String HTTP_SCHEME = "http";
  private static final byte[] EMPTY_BYTES = new byte[0];
  private static final GrapheneHttpServerRuntime DISABLED =
      new GrapheneHttpServerRuntime("", -1, "", null, false);

  private final String host;
  private final int port;
  private final String baseUrl;
  private final HttpServer server;
  private final GrapheneHttpUrls urls;
  private volatile boolean running;

  private GrapheneHttpServerRuntime(
      String host, int port, String baseUrl, HttpServer server, boolean running) {
    this.host = host;
    this.port = port;
    this.baseUrl = baseUrl;
    this.server = server;
    this.running = running;
    this.urls = new GrapheneHttpUrls(this::runningBaseUrl);
  }

  public static GrapheneHttpServerRuntime disabled() {
    return DISABLED;
  }

  public static GrapheneHttpServerRuntime start(Map<String, GrapheneHttpConfig> consumerConfigs) {
    Map<String, GrapheneHttpConfig> validatedConsumerConfigs =
        validateConsumerConfigs(consumerConfigs);
    if (validatedConsumerConfigs.isEmpty()) {
      throw new IllegalArgumentException("consumerConfigs must not be empty");
    }

    MergedHttpServerConfig mergedConfig = mergeServerConfig(validatedConsumerConfigs);
    InetAddress bindAddress = resolveLoopbackAddress(mergedConfig.bindHost());
    HttpServer server = createServer(mergedConfig, bindAddress);
    Map<String, HttpMount> mounts = createMounts(validatedConsumerConfigs);
    server.createContext(PATH_DELIMITER, new RoutingHttpHandler(mounts));
    server.start();

    int boundPort = server.getAddress().getPort();
    String baseUrl = buildBaseUrl(bindAddress.getHostAddress(), boundPort);
    return new GrapheneHttpServerRuntime(
        bindAddress.getHostAddress(), boundPort, baseUrl, server, true);
  }

  private static Map<String, GrapheneHttpConfig> validateConsumerConfigs(
      Map<String, GrapheneHttpConfig> consumerConfigs) {
    Map<String, GrapheneHttpConfig> configs =
        Objects.requireNonNull(consumerConfigs, "consumerConfigs");
    LinkedHashMap<String, GrapheneHttpConfig> validatedConfigs = new LinkedHashMap<>();
    configs.forEach(
        (consumerId, config) ->
            validatedConfigs.put(
                AssetId.normalizeNamespace(consumerId),
                Objects.requireNonNull(config, "consumer config")));
    return Map.copyOf(validatedConfigs);
  }

  private static Map<String, HttpMount> createMounts(
      Map<String, GrapheneHttpConfig> consumerConfigs) {
    LinkedHashMap<String, HttpMount> mounts = new LinkedHashMap<>();
    for (Map.Entry<String, GrapheneHttpConfig> consumerConfigEntry : consumerConfigs.entrySet()) {
      GrapheneHttpConfig httpConfig = consumerConfigEntry.getValue();
      mounts.put(
          consumerConfigEntry.getKey(),
          new HttpMount(
              httpConfig.fileRoot().map(path -> path.toAbsolutePath().normalize()).orElse(null),
              httpConfig
                  .spaFallback()
                  .map(AbstractGrapheneHttpHandler::requireSafeRequestPath)
                  .orElse(null)));
    }

    return Map.copyOf(mounts);
  }

  private static MergedHttpServerConfig mergeServerConfig(
      Map<String, GrapheneHttpConfig> consumerConfigs) {
    OwnedValue<String> selectedBindHost = null;
    OwnedValue<HttpPortBinding> selectedPortBinding = null;

    for (Map.Entry<String, GrapheneHttpConfig> consumerConfigEntry : consumerConfigs.entrySet()) {
      String consumerId = consumerConfigEntry.getKey();
      GrapheneHttpConfig httpConfig = consumerConfigEntry.getValue();

      selectedBindHost =
          mergeOwnedValue(selectedBindHost, httpConfig.bindHost(), consumerId, "HTTP bindHost");
      selectedPortBinding =
          mergeOwnedValue(
              selectedPortBinding, HttpPortBinding.of(httpConfig), consumerId, "HTTP port binding");
    }

    String bindHost = selectedBindHost == null ? "127.0.0.1" : selectedBindHost.value();
    HttpPortBinding portBinding =
        selectedPortBinding == null
            ? HttpPortBinding.of(GrapheneHttpConfig.builder().build())
            : selectedPortBinding.value();
    return new MergedHttpServerConfig(
        bindHost, portBinding.fixedPort(), portBinding.randomPortRange());
  }

  private static <T> OwnedValue<T> mergeOwnedValue(
      OwnedValue<T> selectedValue, T candidateValue, String candidateOwner, String settingName) {
    if (selectedValue == null) {
      return new OwnedValue<>(candidateValue, candidateOwner);
    }

    if (Objects.equals(selectedValue.value(), candidateValue)) {
      return selectedValue;
    }

    throw new IllegalStateException(
        "Conflicting Graphene "
            + settingName
            + " between consumers "
            + selectedValue.owner()
            + " and "
            + candidateOwner);
  }

  private static InetAddress resolveLoopbackAddress(String host) {
    try {
      InetAddress address = InetAddress.getByName(host);
      if (!address.isLoopbackAddress()) {
        throw new IllegalArgumentException(
            "Graphene HTTP bindHost must resolve to loopback: " + host);
      }

      return address;
    } catch (UnknownHostException exception) {
      throw new IllegalArgumentException(
          "Failed to resolve Graphene HTTP bindHost: " + host, exception);
    }
  }

  private static HttpServer createServer(MergedHttpServerConfig config, InetAddress bindAddress) {
    if (config.fixedPort() != null) {
      int port = config.fixedPort();
      try {
        return HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
      } catch (IOException exception) {
        throw new IllegalStateException(
            "Failed to bind Graphene HTTP server on port " + port, exception);
      }
    }

    GrapheneHttpConfig.PortRange portRange =
        Objects.requireNonNull(config.randomPortRange(), "randomPortRange");
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

    throw new IllegalStateException(
        "No available port in range " + minPort + "-" + maxPort + " for Graphene HTTP server");
  }

  private static String buildBaseUrl(String host, int port) {
    try {
      return new URI(HTTP_SCHEME, null, host, port, null, null, null).toASCIIString();
    } catch (URISyntaxException exception) {
      throw new IllegalStateException("Failed to build Graphene HTTP base URL", exception);
    }
  }

  private static InputStream openClasspathResource(String path) {
    ClassLoader classLoader = GrapheneHttpServerRuntime.class.getClassLoader();
    return classLoader.getResourceAsStream(path);
  }

  private static ResourceResponse loadClasspathResource(String normalizedPath) {
    try (InputStream inputStream = openClasspathResource(normalizedPath)) {
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

  public GrapheneHttpUrls urls() {
    return urls;
  }

  private String runningBaseUrl() {
    return running ? baseUrl : "";
  }

  @Override
  public void close() {
    if (!running || server == null) {
      return;
    }

    running = false;
    server.stop(0);
  }

  @SuppressWarnings("java:S6206")
  private static final class ResourceResponse {
    private final int statusCode;
    private final String contentType;
    private final byte[] payload;

    private ResourceResponse(int statusCode, String contentType, byte[] payload) {
      this.statusCode = statusCode;
      this.contentType = contentType;
      this.payload = payload == null ? EMPTY_BYTES : payload;
    }

    private int statusCode() {
      return statusCode;
    }

    private String contentType() {
      return contentType;
    }

    private byte[] payload() {
      return payload;
    }
  }

  private abstract static class AbstractGrapheneHttpHandler implements HttpHandler {
    protected static String normalizeRequestPath(String requestPath) {
      String normalizedPath = requestPath == null ? "" : requestPath.trim();
      while (normalizedPath.startsWith(PATH_DELIMITER)) {
        normalizedPath = normalizedPath.substring(1);
      }

      return normalizedPath;
    }

    protected static String requireSafeRequestPath(String requestPath) {
      String normalizedPath = normalizeRequestPath(requestPath);
      if (hasUnsafeRequestPath(normalizedPath)) {
        throw new IllegalArgumentException("Invalid HTTP resource path: " + requestPath);
      }
      return normalizedPath;
    }

    private static boolean hasUnsafeRequestPath(String requestPath) {
      if (requestPath.isBlank() || requestPath.indexOf('\\') >= 0) {
        return true;
      }

      for (String segment : requestPath.split(PATH_DELIMITER, -1)) {
        if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
          return true;
        }

        for (int index = 0; index < segment.length(); index++) {
          if (Character.isISOControl(segment.charAt(index))) {
            return true;
          }
        }
      }
      return false;
    }

    private static void send(
        HttpExchange exchange,
        int statusCode,
        String contentType,
        byte[] payload,
        boolean headRequest)
        throws IOException {
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set(
          HEADER_CONTENT_TYPE, contentType == null ? CONTENT_TYPE_TEXT_PLAIN : contentType);

      byte[] responsePayload = payload == null ? EMPTY_BYTES : payload;
      if (headRequest) {
        responseHeaders.set("Content-Length", Integer.toString(responsePayload.length));
        exchange.sendResponseHeaders(statusCode, -1);
        return;
      }

      exchange.sendResponseHeaders(statusCode, responsePayload.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(responsePayload);
      }
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
      if (exchange == null) {
        return;
      }

      try (exchange) {
        String method = exchange.getRequestMethod();
        boolean isHeadRequest = "HEAD".equalsIgnoreCase(method);
        boolean isGetRequest = "GET".equalsIgnoreCase(method);
        boolean isPostRequest = "POST".equalsIgnoreCase(method);
        if (!isGetRequest && !isHeadRequest && !isPostRequest) {
          Headers responseHeaders = exchange.getResponseHeaders();
          responseHeaders.set(HEADER_ALLOW, ALLOW_METHODS);
          responseHeaders.set(HEADER_CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN);
          byte[] payload = "Method Not Allowed".getBytes(StandardCharsets.UTF_8);
          send(exchange, 405, CONTENT_TYPE_TEXT_PLAIN, payload, false);
          return;
        }

        String requestPath = normalizeRequestPath(exchange.getRequestURI().getPath());
        if (hasUnsafeRequestPath(requestPath)) {
          send(
              exchange,
              400,
              CONTENT_TYPE_TEXT_PLAIN,
              "Invalid path".getBytes(StandardCharsets.UTF_8),
              isHeadRequest);
          return;
        }

        ResourceResponse response = loadResourceResponse(requestPath, true);
        send(
            exchange,
            response.statusCode(),
            response.contentType(),
            response.payload(),
            isHeadRequest);
      }
    }

    protected abstract ResourceResponse loadResourceResponse(
        String requestPath, boolean allowSpaFallback);
  }

  private static final class RoutingHttpHandler extends AbstractGrapheneHttpHandler {
    private final AssetHttpHandler assetHttpHandler = new AssetHttpHandler();
    private final ModHttpHandler modHttpHandler;

    private RoutingHttpHandler(Map<String, HttpMount> mounts) {
      this.modHttpHandler = new ModHttpHandler(mounts);
    }

    @Override
    protected ResourceResponse loadResourceResponse(String requestPath, boolean allowSpaFallback) {
      if (requestPath.startsWith(ASSETS_PREFIX)) {
        return assetHttpHandler.loadResourceResponse(requestPath, allowSpaFallback);
      }

      if (requestPath.startsWith(MODS_PREFIX)) {
        return modHttpHandler.loadResourceResponse(requestPath, allowSpaFallback);
      }

      return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
    }
  }

  private static final class AssetHttpHandler extends AbstractGrapheneHttpHandler {
    @Override
    protected ResourceResponse loadResourceResponse(String requestPath, boolean allowSpaFallback) {
      return loadClasspathResource(requestPath);
    }
  }

  private static final class ModHttpHandler extends AbstractGrapheneHttpHandler {
    private final Map<String, HttpMount> mounts;

    private ModHttpHandler(Map<String, HttpMount> mounts) {
      this.mounts = Objects.requireNonNull(mounts, "mounts");
    }

    private ResourceResponse loadFileResource(Path fileRoot, String normalizedPath) {
      if (fileRoot == null) {
        return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      Path normalizedRoot = fileRoot.toAbsolutePath().normalize();
      Path normalizedResolvedPath = normalizedRoot.resolve(normalizedPath).normalize();
      if (!normalizedResolvedPath.startsWith(normalizedRoot)
          || !Files.exists(normalizedResolvedPath)) {
        return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      Path realRoot;
      Path realResolvedPath;
      try {
        realRoot = normalizedRoot.toRealPath();
        realResolvedPath = normalizedResolvedPath.toRealPath();
      } catch (IOException ignored) {
        // Failed to resolve real filesystem path.
        return new ResourceResponse(500, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      if (!realResolvedPath.startsWith(realRoot)
          || !Files.isRegularFile(realResolvedPath)
          || !Files.isReadable(realResolvedPath)) {
        return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      try {
        byte[] payload = Files.readAllBytes(realResolvedPath);
        String contentType = GrapheneMimeTypes.resolve(normalizedPath);
        return new ResourceResponse(200, contentType, payload);
      } catch (IOException ignored) {
        // Failed to read filesystem resource payload.
        return new ResourceResponse(500, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }
    }

    private ResourceResponse loadMountedResource(
        String modId, HttpMount mount, String resourcePath) {
      String normalizedPath = normalizeRequestPath(resourcePath);
      if (normalizedPath.isBlank()) {
        return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      ResourceResponse fileSystemResponse = loadFileResource(mount.fileRoot(), normalizedPath);
      if (fileSystemResponse.statusCode() != 404) {
        return fileSystemResponse;
      }

      return loadClasspathResource(toMountedClasspathPath(modId, normalizedPath));
    }

    private String toMountedClasspathPath(String modId, String normalizedPath) {
      return ASSETS_PREFIX + modId + PATH_DELIMITER + normalizedPath;
    }

    private ResourceResponse loadSpaFallbackResource(
        String modId, HttpMount mount, String resourcePath) {
      String normalizedPath = normalizeRequestPath(resourcePath);
      if (normalizedPath.isBlank()) {
        return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      if (normalizedPath.startsWith(ASSETS_PREFIX)) {
        return loadClasspathResource(normalizedPath);
      }

      if (normalizedPath.startsWith(MODS_PREFIX)) {
        return loadResourceResponse(normalizedPath, false);
      }

      return loadMountedResource(modId, mount, normalizedPath);
    }

    @Override
    protected ResourceResponse loadResourceResponse(String requestPath, boolean allowSpaFallback) {
      ModRequestPath modRequestPath = parseRequestPath(requestPath);
      if (modRequestPath == null) {
        return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      HttpMount mount = mounts.get(modRequestPath.modId());
      if (mount == null) {
        return new ResourceResponse(404, CONTENT_TYPE_TEXT_PLAIN, EMPTY_BYTES);
      }

      ResourceResponse directResponse =
          loadMountedResource(modRequestPath.modId(), mount, modRequestPath.resourcePath());
      if (directResponse.statusCode() != 404) {
        return directResponse;
      }

      String spaFallbackResourcePath = mount.spaFallbackResourcePath();
      if (!allowSpaFallback
          || spaFallbackResourcePath == null
          || spaFallbackResourcePath.isBlank()) {
        return directResponse;
      }

      return loadSpaFallbackResource(modRequestPath.modId(), mount, spaFallbackResourcePath);
    }

    private ModRequestPath parseRequestPath(String requestPath) {
      if (!requestPath.startsWith(MODS_PREFIX)) {
        return null;
      }

      String pathWithinMods = requestPath.substring(MODS_PREFIX.length());
      if (pathWithinMods.isBlank()) {
        return null;
      }

      int separatorIndex = pathWithinMods.indexOf(PATH_DELIMITER);
      if (separatorIndex < 0) {
        return new ModRequestPath(pathWithinMods, "");
      }

      String modId = pathWithinMods.substring(0, separatorIndex);
      String resourcePath = pathWithinMods.substring(separatorIndex + 1);
      return new ModRequestPath(modId, resourcePath);
    }
  }

  private record OwnedValue<T>(T value, String owner) {}

  private record HttpPortBinding(Integer fixedPort, GrapheneHttpConfig.PortRange randomPortRange) {
    private static HttpPortBinding of(GrapheneHttpConfig httpConfig) {
      return new HttpPortBinding(
          httpConfig.fixedPort().orElse(null), httpConfig.randomPortRange().orElse(null));
    }
  }

  private record MergedHttpServerConfig(
      String bindHost, Integer fixedPort, GrapheneHttpConfig.PortRange randomPortRange) {}

  private record HttpMount(Path fileRoot, String spaFallbackResourcePath) {}

  private record ModRequestPath(String modId, String resourcePath) {}
}
