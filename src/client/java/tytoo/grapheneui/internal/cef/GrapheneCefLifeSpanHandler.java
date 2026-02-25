package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.mc.McClient;

final class GrapheneCefLifeSpanHandler extends CefLifeSpanHandlerAdapter {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefLifeSpanHandler.class);

    @Override
    public boolean onBeforePopup(CefBrowser browser, CefFrame ignoredFrame, String targetUrl, String targetFrameName) {
        if (browser == null) {
            return false;
        }

        if (targetUrl == null || targetUrl.isBlank()) {
            DEBUG_LOGGER.debug("Blocked popup without target URL");
            return false;
        }

        McClient.runOnMainThread(() -> browser.loadURL(targetUrl));
        DEBUG_LOGGER.debug("Redirected popup request into current browser targetUrl={} targetFrame={}", targetUrl, targetFrameName);
        return true;
    }
}
