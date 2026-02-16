package tytoo.grapheneui.api.surface;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;

/**
 * Interface for listening to loading events from a browser surface.
 * Implementations can override the default methods to handle specific loading events such as start, end, and errors.
 */
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
