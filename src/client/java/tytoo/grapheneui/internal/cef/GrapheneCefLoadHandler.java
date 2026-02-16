package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.mc.McClient;

import java.util.Objects;

final class GrapheneCefLoadHandler extends CefLoadHandlerAdapter {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefLoadHandler.class);

    private final GrapheneLoadEventBus loadEventBus;
    private final GrapheneBridgeRuntime bridgeRuntime;

    GrapheneCefLoadHandler(GrapheneLoadEventBus loadEventBus, GrapheneBridgeRuntime bridgeRuntime) {
        this.loadEventBus = Objects.requireNonNull(loadEventBus, "loadEventBus");
        this.bridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
    }

    private static boolean isMainFrame(CefFrame frame) {
        return frame == null || frame.isMain();
    }

    private static int browserIdentifier(CefBrowser browser) {
        try {
            return browser.getIdentifier();
        } catch (RuntimeException _) {
            return -1;
        }
    }

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        DEBUG_LOGGER.debug(
                "CEF loading state changed browserId={} isLoading={} canGoBack={} canGoForward={}",
                browserIdentifier(browser),
                isLoading,
                canGoBack,
                canGoForward
        );

        McClient.runOnMainThread(() ->
                loadEventBus.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward)
        );
    }

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        DEBUG_LOGGER.debug(
                "CEF load start browserId={} mainFrame={} transitionType={}",
                browserIdentifier(browser),
                isMainFrame(frame),
                transitionType
        );

        McClient.runOnMainThread(() -> {
            loadEventBus.onLoadStart(browser, frame, transitionType);
            if (isMainFrame(frame)) {
                bridgeRuntime.onLoadStart(browser);
            }
        });
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        DEBUG_LOGGER.debug(
                "CEF load end browserId={} mainFrame={} status={}",
                browserIdentifier(browser),
                isMainFrame(frame),
                httpStatusCode
        );

        McClient.runOnMainThread(() -> {
            loadEventBus.onLoadEnd(browser, frame, httpStatusCode);
            if (isMainFrame(frame)) {
                bridgeRuntime.onLoadEnd(browser);
            }
        });
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
        DEBUG_LOGGER.debug(
                "CEF load error browserId={} mainFrame={} code={} failedUrl={} message={}",
                browserIdentifier(browser),
                isMainFrame(frame),
                errorCode,
                failedUrl,
                errorText
        );

        McClient.runOnMainThread(() ->
                loadEventBus.onLoadError(browser, frame, errorCode, errorText, failedUrl)
        );
    }
}
