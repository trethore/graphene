package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.mc.McClient;

import java.util.Objects;

final class GrapheneCefLoadHandler extends CefLoadHandlerAdapter {
    private final GrapheneLoadEventBus loadEventBus;
    private final GrapheneBridgeRuntime bridgeRuntime;

    GrapheneCefLoadHandler(GrapheneLoadEventBus loadEventBus, GrapheneBridgeRuntime bridgeRuntime) {
        this.loadEventBus = Objects.requireNonNull(loadEventBus, "loadEventBus");
        this.bridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
    }

    private static boolean isMainFrame(CefFrame frame) {
        return frame == null || frame.isMain();
    }

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        McClient.runOnMainThread(() ->
                loadEventBus.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward)
        );
    }

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        McClient.runOnMainThread(() -> {
            loadEventBus.onLoadStart(browser, frame, transitionType);
            if (isMainFrame(frame)) {
                bridgeRuntime.onLoadStart(browser);
            }
        });
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        McClient.runOnMainThread(() -> {
            loadEventBus.onLoadEnd(browser, frame, httpStatusCode);
            if (isMainFrame(frame)) {
                bridgeRuntime.onLoadEnd(browser);
            }
        });
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
        McClient.runOnMainThread(() ->
                loadEventBus.onLoadError(browser, frame, errorCode, errorText, failedUrl)
        );
    }
}
