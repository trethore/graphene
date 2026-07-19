package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.url.GrapheneClasspathUrls;
import io.github.trethore.graphene.internal.resource.GrapheneMimeTypes;
import io.github.trethore.graphene.internal.url.GrapheneAppUrls;
import java.io.IOException;
import java.io.InputStream;
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

final class GrapheneClasspathSchemeHandlerFactory implements CefSchemeHandlerFactory {
  @Override
  public CefResourceHandler create(
      CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
    return new ClasspathResourceHandler();
  }

  private static final class ClasspathResourceHandler extends CefResourceHandlerAdapter {
    private static final byte[] EMPTY_RESPONSE = new byte[0];

    private byte[] responseBytes = EMPTY_RESPONSE;
    private String mimeType = "text/plain";
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
      }
      readOffset = 0;
      callback.Continue();
      return true;
    }

    @Override
    public void getResponseHeaders(
        CefResponse response, IntRef responseLength, StringRef redirectUrl) {
      response.setMimeType(mimeType);
      response.setStatus(found ? 200 : 404);
      responseLength.set(responseBytes.length);
    }

    @Override
    public boolean readResponse(
        byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
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
      readOffset = 0;
      found = false;
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
        return input == null
            ? ResourceResult.notFound()
            : ResourceResult.found(input.readAllBytes());
      } catch (IOException exception) {
        return ResourceResult.notFound();
      }
    }

    private record ResourceResult(boolean found, byte[] bytes) {
      private static ResourceResult found(byte[] bytes) {
        return new ResourceResult(true, bytes);
      }

      private static ResourceResult notFound() {
        return new ResourceResult(false, EMPTY_RESPONSE);
      }
    }
  }
}
