package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserLoadCompleted;
import io.github.trethore.graphene.api.browser.BrowserLoadFailed;
import io.github.trethore.graphene.api.browser.BrowserLoadStarted;
import io.github.trethore.graphene.api.browser.BrowserLoadingState;
import io.github.trethore.graphene.internal.bridge.GrapheneBridgeRuntime;
import io.github.trethore.graphene.internal.event.GrapheneLoadEventBus;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

final class GrapheneCefLoadHandler extends CefLoadHandlerAdapter {
  private final GrapheneLoadEventBus eventBus;
  private final GrapheneBridgeRuntime bridgeRuntime;
  private final GrapheneTaskExecutor taskExecutor;

  GrapheneCefLoadHandler(
      GrapheneLoadEventBus eventBus,
      GrapheneBridgeRuntime bridgeRuntime,
      GrapheneTaskExecutor taskExecutor) {
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.bridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
    this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
  }

  @Override
  public void onLoadingStateChange(
      CefBrowser browser, boolean loading, boolean canGoBack, boolean canGoForward) {
    int browserId = identifier(browser);
    taskExecutor.execute(
        () ->
            eventBus.publish(new BrowserLoadingState(browserId, loading, canGoBack, canGoForward)));
  }

  @Override
  public void onLoadStart(
      CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
    GrapheneCefBrowserAdapter browserAdapter = new GrapheneCefBrowserAdapter(browser);
    BrowserLoadStarted event =
        new BrowserLoadStarted(
            identifier(browser),
            frameUrl(frame),
            isMainFrame(frame),
            transitionType == null ? "UNKNOWN" : transitionType.name());
    taskExecutor.execute(
        () -> {
          eventBus.publish(event);
          if (event.mainFrame()) {
            bridgeRuntime.onLoadStart(browserAdapter);
          }
        });
  }

  @Override
  public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
    GrapheneCefBrowserAdapter browserAdapter = new GrapheneCefBrowserAdapter(browser);
    BrowserLoadCompleted event =
        new BrowserLoadCompleted(
            identifier(browser), frameUrl(frame), isMainFrame(frame), httpStatusCode);
    taskExecutor.execute(
        () -> {
          eventBus.publish(event);
          if (event.mainFrame()) {
            bridgeRuntime.onLoadEnd(browserAdapter);
          }
        });
  }

  @Override
  public void onLoadError(
      CefBrowser browser,
      CefFrame frame,
      CefLoadHandler.ErrorCode errorCode,
      String errorText,
      String failedUrl) {
    BrowserLoadFailed event =
        new BrowserLoadFailed(
            identifier(browser),
            failedUrl,
            isMainFrame(frame),
            errorCode == null ? 0 : errorCode.getCode(),
            errorCode == null ? "UNKNOWN" : errorCode.name(),
            errorText);
    taskExecutor.execute(() -> eventBus.publish(event));
  }

  private static int identifier(CefBrowser browser) {
    try {
      return browser.getIdentifier();
    } catch (RuntimeException exception) {
      return -1;
    }
  }

  private static boolean isMainFrame(CefFrame frame) {
    return frame == null || frame.isMain();
  }

  private static String frameUrl(CefFrame frame) {
    return frame == null ? "" : frame.getURL();
  }
}
