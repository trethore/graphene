package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;

final class GrapheneCefRequestHandler extends CefRequestHandlerAdapter {
  private final GrapheneTaskExecutor mainThreadExecutor;

  GrapheneCefRequestHandler(GrapheneTaskExecutor mainThreadExecutor) {
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
  }

  @Override
  public boolean onOpenURLFromTab(
      CefBrowser browser, CefFrame frame, String targetUrl, boolean userGesture) {
    if (browser == null || targetUrl == null || targetUrl.isBlank()) {
      return false;
    }
    mainThreadExecutor.execute(() -> browser.loadURL(targetUrl));
    return true;
  }
}
