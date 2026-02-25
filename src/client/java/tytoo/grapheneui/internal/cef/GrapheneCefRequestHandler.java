package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.mc.McClient;

final class GrapheneCefRequestHandler extends CefRequestHandlerAdapter {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefRequestHandler.class);

    @Override
    public boolean onOpenURLFromTab(CefBrowser browser, CefFrame ignoredFrame, String targetUrl, boolean userGesture) {
        if (browser == null || targetUrl == null || targetUrl.isBlank()) {
            return false;
        }

        McClient.runOnMainThread(() -> browser.loadURL(targetUrl));
        DEBUG_LOGGER.debug("Redirected tab-open navigation into current browser targetUrl={} userGesture={}", targetUrl, userGesture);
        return true;
    }
}
