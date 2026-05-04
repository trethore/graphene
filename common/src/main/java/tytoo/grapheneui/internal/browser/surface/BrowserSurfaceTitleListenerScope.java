package tytoo.grapheneui.internal.browser.surface;

import tytoo.grapheneui.api.surface.GrapheneTitleListener;
import org.cef.browser.CefBrowser;
import tytoo.grapheneui.internal.event.GrapheneTitleEventBus;

import java.util.*;

public final class BrowserSurfaceTitleListenerScope implements AutoCloseable {
    private static final String TITLE_LISTENER_NAME = "titleListener";

    private final CefBrowser browser;
    private final GrapheneTitleEventBus titleEventBus;
    private final Map<GrapheneTitleListener, GrapheneTitleListener> wrappedListenersByListener = new IdentityHashMap<>();
    private boolean closed;

    public BrowserSurfaceTitleListenerScope(CefBrowser browser, GrapheneTitleEventBus titleEventBus) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.titleEventBus = Objects.requireNonNull(titleEventBus, "titleEventBus");
    }

    public void add(GrapheneTitleListener titleListener) {
        Objects.requireNonNull(titleListener, TITLE_LISTENER_NAME);

        GrapheneTitleListener wrappedListener = createScopedTitleListener(titleListener);
        GrapheneTitleListener previousWrappedListener;
        synchronized (wrappedListenersByListener) {
            if (closed) {
                throw new IllegalStateException("Cannot add title listener to a closed BrowserSurface");
            }

            previousWrappedListener = wrappedListenersByListener.put(titleListener, wrappedListener);
        }

        if (previousWrappedListener != null) {
            titleEventBus.unregister(previousWrappedListener);
        }

        titleEventBus.register(wrappedListener);
    }

    public void remove(GrapheneTitleListener titleListener) {
        Objects.requireNonNull(titleListener, TITLE_LISTENER_NAME);

        GrapheneTitleListener wrappedListener;
        synchronized (wrappedListenersByListener) {
            wrappedListener = wrappedListenersByListener.remove(titleListener);
        }

        if (wrappedListener != null) {
            titleEventBus.unregister(wrappedListener);
        }
    }

    @Override
    public void close() {
        List<GrapheneTitleListener> listenersToUnregister;
        synchronized (wrappedListenersByListener) {
            if (closed) {
                return;
            }

            closed = true;
            listenersToUnregister = new ArrayList<>(wrappedListenersByListener.values());
            wrappedListenersByListener.clear();
        }

        for (GrapheneTitleListener listener : listenersToUnregister) {
            titleEventBus.unregister(listener);
        }
    }

    private GrapheneTitleListener createScopedTitleListener(GrapheneTitleListener titleListener) {
        return (eventBrowser, title) -> {
            if (eventBrowser == browser) {
                titleListener.onTitleChange(eventBrowser, title);
            }
        };
    }
}
