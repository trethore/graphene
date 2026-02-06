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

        private InputStream inputStream;
        private String mimeType = MIME_TEXT_PLAIN;

        private static String resolveMimeType(String path) {
            if (path.endsWith(".html") || path.endsWith(".htm")) {
                return "text/html";
            }

            if (path.endsWith(".js")) {
                return "application/javascript";
            }

            if (path.endsWith(".css")) {
                return "text/css";
            }

            if (path.endsWith(".json")) {
                return "application/json";
            }

            if (path.endsWith(".png")) {
                return "image/png";
            }

            if (path.endsWith(".svg")) {
                return "image/svg+xml";
            }

            return MIME_TEXT_PLAIN;
        }

        private static String normalizeResourcePath(String url) {
            String path = url.substring(CLASS_PATH_URL_PREFIX.length());
            int queryIndex = path.indexOf('?');
            if (queryIndex >= 0) {
                path = path.substring(0, queryIndex);
            }

            if (path.startsWith(PATH_DELIMITER)) {
                return path.substring(1);
            }

            return path;
        }

        private static InputStream openResource(String path) {
            ClassLoader classLoader = GrapheneClasspathSchemeHandlerFactory.class.getClassLoader();
            InputStream stream = classLoader.getResourceAsStream(path);
            if (stream != null || path.startsWith(ASSETS_PREFIX)) {
                return stream;
            }

            return classLoader.getResourceAsStream(ASSETS_FALLBACK_PREFIX + path);
        }

        @Override
        public boolean processRequest(CefRequest request, CefCallback callback) {
            String resourcePath = normalizeResourcePath(request.getURL());
            inputStream = openResource(resourcePath);
            if (inputStream != null) {
                mimeType = resolveMimeType(resourcePath);
            }

            callback.Continue();
            return true;
        }

        @Override
        public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
            response.setMimeType(mimeType);
            if (inputStream == null) {
                response.setStatus(404);
                responseLength.set(0);
                return;
            }

            response.setStatus(200);
            try {
                responseLength.set(inputStream.available());
            } catch (IOException _) {
                responseLength.set(0);
            }
        }

        @Override
        public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
            if (inputStream == null) {
                return false;
            }

            try {
                int bytes = inputStream.read(dataOut, 0, bytesToRead);
                if (bytes == -1) {
                    closeInputStreamQuietly();
                    return false;
                }

                bytesRead.set(bytes);
                return true;
            } catch (IOException _) {
                closeInputStreamQuietly();
                return false;
            }
        }

        @Override
        public void cancel() {
            closeInputStreamQuietly();
        }

        private void closeInputStreamQuietly() {
            if (inputStream == null) {
                return;
            }

            try {
                inputStream.close();
            } catch (IOException _) {
                GrapheneCore.LOGGER.debug("Failed to close classpath resource stream");
            } finally {
                inputStream = null;
            }
        }
    }
}
