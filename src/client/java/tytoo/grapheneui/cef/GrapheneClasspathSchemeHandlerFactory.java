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
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.GrapheneCore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

public final class GrapheneClasspathSchemeHandlerFactory implements CefSchemeHandlerFactory {
    private static final String MIME_TEXT_PLAIN = "text/plain";
    private static final String PATH_DELIMITER = "/";
    private static final String ASSETS_PREFIX = "assets" + PATH_DELIMITER;

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        return new ClasspathResourceHandler();
    }

    private static final class ClasspathResourceHandler extends CefResourceHandlerAdapter {
        private static final String ASSETS_FALLBACK_PREFIX = ASSETS_PREFIX + GrapheneCore.ID + PATH_DELIMITER;
        private static final byte[] EMPTY_RESPONSE_BYTES = new byte[0];
        private static final MimeTypeRule[] MIME_TYPE_RULES = {
                new MimeTypeRule(".html", "text/html"),
                new MimeTypeRule(".htm", "text/html"),
                new MimeTypeRule(".js", "application/javascript"),
                new MimeTypeRule(".mjs", "text/javascript"),
                new MimeTypeRule(".css", "text/css"),
                new MimeTypeRule(".json", "application/json"),
                new MimeTypeRule(".png", "image/png"),
                new MimeTypeRule(".jpg", "image/jpeg"),
                new MimeTypeRule(".jpeg", "image/jpeg"),
                new MimeTypeRule(".gif", "image/gif"),
                new MimeTypeRule(".webp", "image/webp"),
                new MimeTypeRule(".ico", "image/x-icon"),
                new MimeTypeRule(".svg", "image/svg+xml"),
                new MimeTypeRule(".woff", "font/woff"),
                new MimeTypeRule(".woff2", "font/woff2"),
                new MimeTypeRule(".ttf", "font/ttf"),
                new MimeTypeRule(".otf", "font/otf"),
                new MimeTypeRule(".wasm", "application/wasm")
        };

        private byte[] responseBytes = EMPTY_RESPONSE_BYTES;
        private int readOffset;
        private boolean resourceFound;
        private String mimeType = MIME_TEXT_PLAIN;

        private static String resolveMimeType(String path) {
            String normalizedPath = path.toLowerCase(Locale.ROOT);
            for (MimeTypeRule mimeTypeRule : MIME_TYPE_RULES) {
                if (normalizedPath.endsWith(mimeTypeRule.extension())) {
                    return mimeTypeRule.mimeType();
                }
            }

            return MIME_TEXT_PLAIN;
        }

        private static String normalizeResourcePath(String url) {
            return GrapheneClasspathUrls.normalizeResourcePath(url);
        }

        private static InputStream openResource(String path) {
            ClassLoader classLoader = GrapheneClasspathSchemeHandlerFactory.class.getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(path);
            if (stream != null || path.startsWith(ASSETS_PREFIX)) {
                return stream;
            }

            return classLoader.getResourceAsStream(ASSETS_FALLBACK_PREFIX + path);
        }

        private static ResourceBytesResult readResourceBytes(String path) {
            try (InputStream inputStream = openResource(path)) {
                if (inputStream == null) {
                    return ResourceBytesResult.notFound();
                }

                return ResourceBytesResult.found(inputStream.readAllBytes());
            } catch (IOException exception) {
                GrapheneCore.LOGGER.debug("Failed to read classpath resource {}", path, exception);
                return ResourceBytesResult.notFound();
            }
        }

        @Override
        public boolean processRequest(CefRequest request, CefCallback callback) {
            String resourcePath = normalizeResourcePath(request.getURL());
            ResourceBytesResult resourceBytesResult = readResourceBytes(resourcePath);
            responseBytes = resourceBytesResult.bytes();
            resourceFound = resourceBytesResult.found();
            readOffset = 0;
            if (resourceFound) {
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
            if (!resourceFound) {
                response.setStatus(404);
                responseLength.set(0);
                return;
            }

            response.setStatus(200);
            responseLength.set(responseBytes.length);
        }

        @Override
        public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
            if (!resourceFound) {
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
            responseBytes = EMPTY_RESPONSE_BYTES;
            readOffset = 0;
            resourceFound = false;
        }

        private record MimeTypeRule(String extension, String mimeType) {
        }

        private record ResourceBytesResult(boolean found, byte[] bytes) {
            private ResourceBytesResult {
                bytes = bytes == null ? EMPTY_RESPONSE_BYTES : bytes;
            }

            private static ResourceBytesResult found(byte[] bytes) {
                return new ResourceBytesResult(true, bytes);
            }

            private static ResourceBytesResult notFound() {
                return new ResourceBytesResult(false, EMPTY_RESPONSE_BYTES);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) {
                    return true;
                }

                if (!(object instanceof ResourceBytesResult(boolean otherFound, byte[] otherBytes))) {
                    return false;
                }

                return found == otherFound && Arrays.equals(bytes, otherBytes);
            }

            @Override
            public int hashCode() {
                int result = Boolean.hashCode(found);
                result = 31 * result + Arrays.hashCode(bytes);
                return result;
            }

            @Override
            public @NonNull String toString() {
                return "ResourceBytesResult[found=" + found + ", bytes=" + Arrays.toString(bytes) + "]";
            }
        }
    }
}
