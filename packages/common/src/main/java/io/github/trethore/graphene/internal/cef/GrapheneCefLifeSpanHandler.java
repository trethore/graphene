package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;

final class GrapheneCefLifeSpanHandler extends CefLifeSpanHandlerAdapter {
  private final GrapheneTaskExecutor mainThreadExecutor;

  GrapheneCefLifeSpanHandler(GrapheneTaskExecutor mainThreadExecutor) {
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
  }

  @Override
  public void onAfterCreated(CefBrowser browser) {
    if (browser instanceof GrapheneCefBrowserSession session) {
      session.initializeBrowserOptions();
    }
  }

  @Override
  public boolean onBeforePopup(
      CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
    if (browser == null) {
      return false;
    }
    if (targetUrl == null || targetUrl.isBlank()) {
      return true;
    }
    mainThreadExecutor.execute(() -> browser.loadURL(targetUrl));
    return true;
  }
}
