package tytoo.grapheneui.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;
import tytoo.grapheneui.bridge.internal.GrapheneBridgeRuntime;
import tytoo.grapheneui.event.GrapheneLoadEventBus;

import java.util.Objects;

final class GrapheneCefLoadHandler extends CefLoadHandlerAdapter {
    private final GrapheneLoadEventBus loadEventBus;
    private final GrapheneBridgeRuntime bridgeRuntime;

    GrapheneCefLoadHandler(GrapheneLoadEventBus loadEventBus, GrapheneBridgeRuntime bridgeRuntime) {
        this.loadEventBus = Objects.requireNonNull(loadEventBus, "loadEventBus");
        this.bridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
    }

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        loadEventBus.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    }

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        loadEventBus.onLoadStart(browser, frame, transitionType);
        bridgeRuntime.onLoadStart(browser);
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        loadEventBus.onLoadEnd(browser, frame, httpStatusCode);
        bridgeRuntime.onLoadEnd(browser);
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
        loadEventBus.onLoadError(browser, frame, errorCode, errorText, failedUrl);
    }
}
