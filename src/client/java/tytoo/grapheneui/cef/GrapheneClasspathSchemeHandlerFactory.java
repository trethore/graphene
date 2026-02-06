package tytoo.grapheneui.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCallback;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import tytoo.grapheneui.GrapheneCore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class GrapheneClasspathSchemeHandlerFactory implements CefSchemeHandlerFactory {
    private static final String MIME_TEXT_PLAIN = "text/plain";
    private static final String CLASS_PATH_URL_PREFIX = "classpath://";
    private static final String PATH_DELIMITER = "/";
    private static final String ASSETS_PREFIX = "assets" + PATH_DELIMITER;

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        return new ClasspathResourceHandler();
    }

    private static final class ClasspathResourceHandler extends CefResourceHandlerAdapter {
        private static final String ASSETS_FALLBACK_PREFIX = ASSETS_PREFIX + GrapheneCore.ID + PATH_DELIMITER;

        private byte[] responseBytes;
        private int readOffset;
        private String mimeType = MIME_TEXT_PLAIN;

        private static String resolveMimeType(String path) {
            String normalizedPath = path.toLowerCase(Locale.ROOT);
            if (normalizedPath.endsWith(".html") || normalizedPath.endsWith(".htm")) {
                return "text/html";
            }

            if (normalizedPath.endsWith(".js")) {
                return "application/javascript";
            }

            if (normalizedPath.endsWith(".mjs")) {
                return "text/javascript";
            }

            if (normalizedPath.endsWith(".css")) {
                return "text/css";
            }

            if (normalizedPath.endsWith(".json")) {
                return "application/json";
            }

            if (normalizedPath.endsWith(".png")) {
                return "image/png";
            }

            if (normalizedPath.endsWith(".jpg") || normalizedPath.endsWith(".jpeg")) {
                return "image/jpeg";
            }

            if (normalizedPath.endsWith(".gif")) {
                return "image/gif";
            }

            if (normalizedPath.endsWith(".webp")) {
                return "image/webp";
            }

            if (normalizedPath.endsWith(".ico")) {
                return "image/x-icon";
            }

            if (normalizedPath.endsWith(".svg")) {
                return "image/svg+xml";
            }

            if (normalizedPath.endsWith(".woff")) {
                return "font/woff";
            }

            if (normalizedPath.endsWith(".woff2")) {
                return "font/woff2";
            }

            if (normalizedPath.endsWith(".ttf")) {
                return "font/ttf";
            }

            if (normalizedPath.endsWith(".otf")) {
                return "font/otf";
            }

            if (normalizedPath.endsWith(".wasm")) {
                return "application/wasm";
            }

            return MIME_TEXT_PLAIN;
        }

        private static String normalizeResourcePath(String url) {
            String path = url.substring(CLASS_PATH_URL_PREFIX.length());
            int queryIndex = path.indexOf('?');
            if (queryIndex >= 0) {
                path = path.substring(0, queryIndex);
            }

            String normalizedPath = path;
            if (normalizedPath.startsWith(PATH_DELIMITER)) {
                normalizedPath = normalizedPath.substring(1);
            }

            return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
        }

        private static InputStream openResource(String path) {
            ClassLoader classLoader = GrapheneClasspathSchemeHandlerFactory.class.getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(path);
            if (stream != null || path.startsWith(ASSETS_PREFIX)) {
                return stream;
            }

            return classLoader.getResourceAsStream(ASSETS_FALLBACK_PREFIX + path);
        }

        private static byte[] readResourceBytes(String path) {
            try (InputStream inputStream = openResource(path)) {
                if (inputStream == null) {
                    return null;
                }

                return inputStream.readAllBytes();
            } catch (IOException exception) {
                GrapheneCore.LOGGER.debug("Failed to read classpath resource {}", path, exception);
                return null;
            }
        }

        @Override
        public boolean processRequest(CefRequest request, CefCallback callback) {
            String resourcePath = normalizeResourcePath(request.getURL());
            responseBytes = readResourceBytes(resourcePath);
            readOffset = 0;
            if (responseBytes != null) {
                mimeType = resolveMimeType(resourcePath);
            } else {
                mimeType = MIME_TEXT_PLAIN;
            }

            callback.Continue();
            return true;
        }

        @Override
        public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
            response.setMimeType(mimeType);
            if (responseBytes == null) {
                response.setStatus(404);
                responseLength.set(0);
                return;
            }

            response.setStatus(200);
            responseLength.set(responseBytes.length);
        }

        @Override
        public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
            if (responseBytes == null) {
                bytesRead.set(0);
                return false;
            }

            int remainingBytes = responseBytes.length - readOffset;
            if (remainingBytes <= 0) {
                bytesRead.set(0);
                return false;
            }

            int bytesToCopy = Math.min(bytesToRead, remainingBytes);
            System.arraycopy(responseBytes, readOffset, dataOut, 0, bytesToCopy);
            readOffset += bytesToCopy;
            bytesRead.set(bytesToCopy);
            return true;
        }

        @Override
        public void cancel() {
            responseBytes = null;
            readOffset = 0;
        }
    }
}
