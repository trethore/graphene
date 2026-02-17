package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.Objects;

final class GrapheneCefMessageRouterHandler extends CefMessageRouterHandlerAdapter {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefMessageRouterHandler.class);

    private final GrapheneBridgeRuntime bridgeRuntime;

    GrapheneCefMessageRouterHandler(GrapheneBridgeRuntime bridgeRuntime) {
        this.bridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
    }

    private static int browserIdentifier(CefBrowser browser) {
        try {
            return browser.getIdentifier();
        } catch (RuntimeException ignored) {
            return -1;
        }
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
        boolean handled = bridgeRuntime.onQuery(browser, request, callback);
        DEBUG_LOGGER.debugIfEnabled(logger -> {
            int requestSize = request == null ? 0 : request.length();
            logger.debug(
                    "CEF query browserId={} queryId={} persistent={} requestSize={} handled={}",
                    browserIdentifier(browser),
                    queryId,
                    persistent,
                    requestSize,
                    handled
            );
        });

        return handled;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        bridgeRuntime.onQueryCanceled(browser);
        DEBUG_LOGGER.debug("CEF query canceled browserId={} queryId={}", browserIdentifier(browser), queryId);
    }
}
