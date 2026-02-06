package tytoo.grapheneui.client.event;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GrapheneLoadEventBus {
    private final List<GrapheneLoadListener> listeners = new CopyOnWriteArrayList<>();

    public void register(GrapheneLoadListener listener) {
        listeners.add(listener);
    }

    public void unregister(GrapheneLoadListener listener) {
        listeners.remove(listener);
    }

    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        for (GrapheneLoadListener listener : listeners) {
            listener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
        }
    }

    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        for (GrapheneLoadListener listener : listeners) {
            listener.onLoadStart(browser, frame, transitionType);
        }
    }

    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        for (GrapheneLoadListener listener : listeners) {
            listener.onLoadEnd(browser, frame, httpStatusCode);
        }
    }

    public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
        for (GrapheneLoadListener listener : listeners) {
            listener.onLoadError(browser, frame, errorCode, errorText, failedUrl);
        }
    }
}
