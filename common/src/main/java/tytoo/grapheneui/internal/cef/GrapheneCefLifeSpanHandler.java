package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import tytoo.grapheneui.internal.core.GrapheneMainThreadExecutor;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.Objects;

final class GrapheneCefLifeSpanHandler extends CefLifeSpanHandlerAdapter {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefLifeSpanHandler.class);

    private final GrapheneCefBrowserShutdownTracker shutdownTracker;
    private final GrapheneMainThreadExecutor mainThreadExecutor;

    GrapheneCefLifeSpanHandler(GrapheneCefBrowserShutdownTracker shutdownTracker) {
        this(shutdownTracker, GrapheneMainThreadExecutor.DIRECT);
    }

    GrapheneCefLifeSpanHandler(
            GrapheneCefBrowserShutdownTracker shutdownTracker,
            GrapheneMainThreadExecutor mainThreadExecutor
    ) {
        this.shutdownTracker = Objects.requireNonNull(shutdownTracker, "shutdownTracker");
        this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    }

    @Override
    public boolean onBeforePopup(CefBrowser browser, CefFrame ignoredFrame, String targetUrl, String targetFrameName) {
        if (browser == null) {
            return false;
        }

        if (targetUrl == null || targetUrl.isBlank()) {
            DEBUG_LOGGER.debug("Blocked popup without target URL");
            return true;
        }

        mainThreadExecutor.run(() -> browser.loadURL(targetUrl));
        DEBUG_LOGGER.debug("Redirected popup request into current browser targetUrl={} targetFrame={}", targetUrl, targetFrameName);
        return true;
    }

    @Override
    public void onAfterCreated(CefBrowser browser) {
        shutdownTracker.onAfterCreated(browser);
    }

    @Override
    public void onBeforeClose(CefBrowser browser) {
        shutdownTracker.onBeforeClose(browser);
    }
}
