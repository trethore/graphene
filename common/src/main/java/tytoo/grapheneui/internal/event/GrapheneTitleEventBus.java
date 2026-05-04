package tytoo.grapheneui.internal.event;

import org.cef.browser.CefBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.surface.GrapheneTitleListener;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GrapheneTitleEventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneTitleEventBus.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneTitleEventBus.class);
    private static final String TITLE_CHANGE_EVENT_NAME = "onTitleChange";

    private final List<GrapheneTitleListener> listeners = new CopyOnWriteArrayList<>();

    public void register(GrapheneTitleListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void unregister(GrapheneTitleListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    public void clear() {
        listeners.clear();
    }

    public void onTitleChange(CefBrowser browser, String title) {
        dispatch(listener -> listener.onTitleChange(browser, title));
    }

    private void dispatch(ListenerCallback callback) {
        DEBUG_LOGGER.debug("Dispatching Graphene title event {} to {} listener(s)", TITLE_CHANGE_EVENT_NAME, listeners.size());

        for (GrapheneTitleListener listener : listeners) {
            try {
                callback.dispatch(listener);
            } catch (RuntimeException exception) {
                LOGGER.error("Unhandled GrapheneTitleListener exception during {}", TITLE_CHANGE_EVENT_NAME, exception);
            }
        }
    }

    @FunctionalInterface
    private interface ListenerCallback {
        void dispatch(GrapheneTitleListener listener);
    }
}
