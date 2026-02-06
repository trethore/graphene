package tytoo.grapheneui.client.cef;

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

import java.io.InputStream;

public final class GrapheneClasspathSchemeHandlerFactory implements CefSchemeHandlerFactory {
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

        return "text/plain";
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        return new CefResourceHandlerAdapter() {
            private InputStream inputStream;
            private String mimeType = "text/plain";

            @Override
            public boolean processRequest(CefRequest request, CefCallback callback) {
                String path = request.getURL().substring("classpath://".length());
                int queryIndex = path.indexOf('?');
                if (queryIndex >= 0) {
                    path = path.substring(0, queryIndex);
                }

                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                ClassLoader classLoader = getClass().getClassLoader();
                inputStream = classLoader.getResourceAsStream(path);
                if (inputStream == null && !path.startsWith("assets/")) {
                    String fallbackPath = "assets/" + GrapheneCore.ID + "/" + path;
                    inputStream = classLoader.getResourceAsStream(fallbackPath);
                }

                if (inputStream != null) {
                    mimeType = resolveMimeType(path);
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
                } catch (Exception exception) {
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
                        inputStream.close();
                        inputStream = null;
                        return false;
                    }

                    bytesRead.set(bytes);
                    return true;
                } catch (Exception exception) {
                    return false;
                }
            }

            @Override
            public void cancel() {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception ignored) {
                }
            }
        };
    }
}
