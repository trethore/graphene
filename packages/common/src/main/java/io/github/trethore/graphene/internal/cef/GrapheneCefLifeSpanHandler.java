package io.github.trethore.graphene.internal.cef;

import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefWindowOpenDisposition;

final class GrapheneCefLifeSpanHandler extends CefLifeSpanHandlerAdapter {
  private final GrapheneCefNavigationRouter navigationRouter;

  GrapheneCefLifeSpanHandler(GrapheneCefNavigationRouter navigationRouter) {
    this.navigationRouter = Objects.requireNonNull(navigationRouter, "navigationRouter");
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
    navigationRouter.onPopup(
        browser, frame, targetUrl, targetFrameName, CefWindowOpenDisposition.UNKNOWN, false);
    return true;
  }

  @Override
  public boolean onBeforePopup(
      CefBrowser browser,
      CefFrame frame,
      int popupId,
      String targetUrl,
      String targetFrameName,
      CefWindowOpenDisposition targetDisposition,
      boolean userGesture) {
    navigationRouter.onPopup(
        browser, frame, targetUrl, targetFrameName, targetDisposition, userGesture);
    return true;
  }
}
