package tytoo.grapheneui.internal.event;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.surface.GrapheneLoadListener;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GrapheneLoadEventBus {
    private final List<GrapheneLoadListener> listeners = new CopyOnWriteArrayList<>();

    public void register(GrapheneLoadListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void unregister(GrapheneLoadListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    public void clear() {
        listeners.clear();
    }

    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        dispatch("onLoadingStateChange", listener -> listener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward));
    }

    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        dispatch("onLoadStart", listener -> listener.onLoadStart(browser, frame, transitionType));
    }

    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        dispatch("onLoadEnd", listener -> listener.onLoadEnd(browser, frame, httpStatusCode));
    }

    public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
        dispatch("onLoadError", listener -> listener.onLoadError(browser, frame, errorCode, errorText, failedUrl));
    }

    private void dispatch(String eventName, ListenerCallback callback) {
        for (GrapheneLoadListener listener : listeners) {
            try {
                callback.dispatch(listener);
            } catch (RuntimeException exception) {
                GrapheneCore.LOGGER.error("Unhandled GrapheneLoadListener exception during {}", eventName, exception);
            }
        }
    }

    @FunctionalInterface
    private interface ListenerCallback {
        void dispatch(GrapheneLoadListener listener);
    }
}
