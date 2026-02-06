package tytoo.grapheneui.client.event;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;

public interface GrapheneLoadListener {
    default void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
    }

    default void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
    }

    default void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
    }

    default void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
    }
}
