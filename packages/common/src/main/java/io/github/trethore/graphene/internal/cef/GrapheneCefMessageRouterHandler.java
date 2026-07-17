package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.bridge.BridgeFrame;
import io.github.trethore.graphene.internal.bridge.GrapheneBridgeRuntime;
import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

final class GrapheneCefMessageRouterHandler extends CefMessageRouterHandlerAdapter {
  private final GrapheneBridgeRuntime bridgeRuntime;

  GrapheneCefMessageRouterHandler(GrapheneBridgeRuntime bridgeRuntime) {
    this.bridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
  }

  @Override
  public boolean onQuery(
      CefBrowser browser,
      CefFrame frame,
      long queryId,
      String request,
      boolean persistent,
      CefQueryCallback callback) {
    return bridgeRuntime.onQuery(
        new GrapheneCefBrowserAdapter(browser),
        frame(frame),
        request,
        new GrapheneCefQueryCallbackAdapter(callback));
  }

  private static BridgeFrame frame(CefFrame frame) {
    if (frame == null) {
      return new BridgeFrame("", false);
    }
    try {
      return new BridgeFrame(value(frame.getURL()), frame.isMain());
    } catch (RuntimeException exception) {
      return new BridgeFrame("", false);
    }
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
