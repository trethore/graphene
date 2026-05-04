package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import tytoo.grapheneui.internal.core.GrapheneMainThreadExecutor;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.Objects;

final class GrapheneCefRequestHandler extends CefRequestHandlerAdapter {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefRequestHandler.class);

    private final GrapheneMainThreadExecutor mainThreadExecutor;

    GrapheneCefRequestHandler() {
        this(GrapheneMainThreadExecutor.DIRECT);
    }

    GrapheneCefRequestHandler(GrapheneMainThreadExecutor mainThreadExecutor) {
        this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    }

    @Override
    public boolean onOpenURLFromTab(CefBrowser browser, CefFrame ignoredFrame, String targetUrl, boolean userGesture) {
        if (browser == null || targetUrl == null || targetUrl.isBlank()) {
            return false;
        }

        mainThreadExecutor.run(() -> browser.loadURL(targetUrl));
        DEBUG_LOGGER.debug("Redirected tab-open navigation into current browser targetUrl={} userGesture={}", targetUrl, userGesture);
        return true;
    }
}
