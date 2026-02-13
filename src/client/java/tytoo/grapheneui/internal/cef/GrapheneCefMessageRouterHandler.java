package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;

import java.util.Objects;

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
            CefQueryCallback callback
    ) {
        return bridgeRuntime.onQuery(browser, request, callback);
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        bridgeRuntime.onQueryCanceled(browser);
    }
}
