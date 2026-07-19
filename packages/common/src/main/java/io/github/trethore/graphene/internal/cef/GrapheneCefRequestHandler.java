package io.github.trethore.graphene.internal.cef;

import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefWindowOpenDisposition;
import org.cef.network.CefRequest;

final class GrapheneCefRequestHandler extends CefRequestHandlerAdapter {
  private final GrapheneCefNavigationRouter navigationRouter;

  GrapheneCefRequestHandler(GrapheneCefNavigationRouter navigationRouter) {
    this.navigationRouter = Objects.requireNonNull(navigationRouter, "navigationRouter");
  }

  @Override
  public boolean onBeforeBrowse(
      CefBrowser browser,
      CefFrame frame,
      CefRequest request,
      boolean userGesture,
      boolean isRedirect) {
    if (request == null) {
      return false;
    }
    return navigationRouter.onMainFrameNavigation(
        browser, frame, request.getURL(), userGesture, isRedirect);
  }

  @Override
  public boolean onOpenURLFromTab(
      CefBrowser browser, CefFrame frame, String targetUrl, boolean userGesture) {
    return navigationRouter.onOpenFromTab(
        browser, frame, targetUrl, CefWindowOpenDisposition.UNKNOWN, userGesture);
  }

  @Override
  public boolean onOpenURLFromTab(
      CefBrowser browser,
      CefFrame frame,
      String targetUrl,
      CefWindowOpenDisposition targetDisposition,
      boolean userGesture) {
    return navigationRouter.onOpenFromTab(
        browser, frame, targetUrl, targetDisposition, userGesture);
  }
}
