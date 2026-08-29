package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.url.GrapheneClasspathUrls;
import io.github.trethore.graphene.internal.resource.GrapheneByteRange;
import io.github.trethore.graphene.internal.resource.GrapheneMimeTypes;
import io.github.trethore.graphene.internal.url.GrapheneAppUrls;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
import org.jetbrains.annotations.NotNull;

final class GrapheneClasspathSchemeHandlerFactory implements CefSchemeHandlerFactory {
    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        return new ClasspathResourceHandler();
    }

    private static final class ClasspathResourceHandler extends CefResourceHandlerAdapter {
        private static final byte[] EMPTY_RESPONSE = new byte[0];

        private byte[] responseBytes = EMPTY_RESPONSE;
        private String mimeType = "text/plain";
        private String contentRange = "";
        private int statusCode = 404;
        private int readOffset;
        private boolean found;

        @Override
        public boolean processRequest(CefRequest request, CefCallback callback) {
            String resourcePath = normalizeResourcePath(request.getURL());
            ResourceResult resource = readResource(resourcePath);
            responseBytes = resource.bytes();
            found = resource.found();
            if (found) {
                mimeType = GrapheneMimeTypes.resolve(resourcePath);
                applyByteRange(request.getHeaderByName("Range"), resource.bytes());
            } else {
                statusCode = 404;
                contentRange = "";
            }
            readOffset = 0;
            callback.Continue();
            return true;
        }

        @Override
        public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
            response.setMimeType(mimeType);
            response.setStatus(statusCode);
            if (found) {
                response.setHeaderByName("Accept-Ranges", "bytes", true);
            }
            if (!contentRange.isEmpty()) {
                response.setHeaderByName("Content-Range", contentRange, true);
            }
            responseLength.set(responseBytes.length);
        }

        @Override
        public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
            int remaining = responseBytes.length - readOffset;
            if (remaining <= 0) {
                bytesRead.set(0);
                return false;
            }
            int copiedBytes = Math.min(bytesToRead, remaining);
            System.arraycopy(responseBytes, readOffset, dataOut, 0, copiedBytes);
            readOffset += copiedBytes;
            bytesRead.set(copiedBytes);
            return true;
        }

        @Override
        public void cancel() {
            responseBytes = EMPTY_RESPONSE;
            contentRange = "";
            statusCode = 404;
            readOffset = 0;
            found = false;
        }

        private void applyByteRange(String rangeHeader, byte[] resourceBytes) {
            GrapheneByteRange.Resolution range = GrapheneByteRange.resolve(rangeHeader, resourceBytes.length);
            contentRange = range.contentRange();
            if (range.status() == GrapheneByteRange.Status.UNSATISFIABLE) {
                responseBytes = EMPTY_RESPONSE;
                statusCode = 416;
                return;
            }
            if (range.status() == GrapheneByteRange.Status.PARTIAL) {
                responseBytes = Arrays.copyOfRange(resourceBytes, range.startInclusive(), range.endExclusive());
                statusCode = 206;
                return;
            }

            responseBytes = resourceBytes;
            statusCode = 200;
        }

        private static String normalizeResourcePath(String url) {
            String appPath = GrapheneAppUrls.normalizeResourcePath(url);
            return appPath.isBlank() ? GrapheneClasspathUrls.normalizeResourcePath(url) : appPath;
        }

        private static ResourceResult readResource(String path) {
            if (path.isBlank()) {
                return ResourceResult.notFound();
            }
            try (InputStream input =
                    GrapheneClasspathSchemeHandlerFactory.class.getClassLoader().getResourceAsStream(path)) {
                return input == null ? ResourceResult.notFound() : ResourceResult.found(input.readAllBytes());
            } catch (IOException exception) {
                return ResourceResult.notFound();
            }
        }

        private record ResourceResult(boolean found, byte[] bytes) {
            @Override
            public boolean equals(Object object) {
                return this == object
                        || object instanceof ResourceResult(boolean found1, byte[] bytes1)
                                && found == found1
                                && Arrays.equals(bytes, bytes1);
            }

            @Override
            public int hashCode() {
                return 31 * Boolean.hashCode(found) + Arrays.hashCode(bytes);
            }

            @Override
            public @NotNull String toString() {
                return "ResourceResult[found=" + found + ", bytes=" + Arrays.toString(bytes) + "]";
            }

            private static ResourceResult found(byte[] bytes) {
                return new ResourceResult(true, bytes);
            }

            private static ResourceResult notFound() {
                return new ResourceResult(false, EMPTY_RESPONSE);
            }
        }
    }
}
