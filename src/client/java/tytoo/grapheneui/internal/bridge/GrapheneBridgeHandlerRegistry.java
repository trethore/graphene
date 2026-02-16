package tytoo.grapheneui.internal.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.bridge.GrapheneBridgeEventListener;
import tytoo.grapheneui.api.bridge.GrapheneBridgeRequestHandler;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

final class GrapheneBridgeHandlerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgeHandlerRegistry.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneBridgeHandlerRegistry.class);

    private final GrapheneBridgeDiagnostics diagnostics;
    private final Map<String, CopyOnWriteArrayList<GrapheneBridgeEventListener>> eventListenersByChannel = new ConcurrentHashMap<>();
    private final Map<String, GrapheneBridgeRequestHandler> requestHandlersByChannel = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Runnable> readyListeners = new CopyOnWriteArrayList<>();

    GrapheneBridgeHandlerRegistry(GrapheneBridgeDiagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    GrapheneBridgeSubscription onReady(Runnable listener, boolean ready) {
        readyListeners.add(listener);
        DEBUG_LOGGER.debug("Registered bridge ready listener total={} readyNow={}", readyListeners.size(), ready);
        if (ready) {
            runReadyListener(listener);
        }

        return () -> readyListeners.remove(listener);
    }

    GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
        CopyOnWriteArrayList<GrapheneBridgeEventListener> listeners = eventListenersByChannel.computeIfAbsent(
                channel,
                _ -> new CopyOnWriteArrayList<>()
        );
        listeners.add(listener);
        DEBUG_LOGGER.debug("Registered bridge event listener channel={} totalForChannel={}", channel, listeners.size());
        return () -> removeEventListener(channel, listener);
    }

    GrapheneBridgeSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler) {
        GrapheneBridgeRequestHandler previousHandler = requestHandlersByChannel.put(channel, handler);
        if (previousHandler != null && previousHandler != handler) {
            LOGGER.warn("Replacing existing Graphene bridge request handler for channel {}", channel);
            diagnostics.onRequestHandlerReplaced(channel);
        }

        DEBUG_LOGGER.debug("Registered bridge request handler channel={} replaced={}", channel, previousHandler != null);
        return () -> requestHandlersByChannel.remove(channel, handler);
    }

    GrapheneBridgeRequestHandler requestHandler(String channel) {
        return requestHandlersByChannel.get(channel);
    }

    void dispatchEvent(String channel, String payloadJson) {
        List<GrapheneBridgeEventListener> listeners = eventListenersByChannel.get(channel);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        for (GrapheneBridgeEventListener listener : listeners) {
            try {
                listener.onEvent(channel, payloadJson);
            } catch (RuntimeException exception) {
                LOGGER.warn("Graphene bridge event listener failed for channel {}", channel, exception);
            }
        }

        DEBUG_LOGGER.debugIfEnabled(logger -> {
            int payloadSize = payloadJson == null ? 0 : payloadJson.length();
            logger.debug("Dispatched bridge event channel={} listeners={} payloadSize={}", channel, listeners.size(), payloadSize);
        });
    }

    void notifyReady() {
        DEBUG_LOGGER.debug("Notifying {} bridge ready listener(s)", readyListeners.size());

        for (Runnable listener : readyListeners) {
            runReadyListener(listener);
        }
    }

    void clear() {
        eventListenersByChannel.clear();
        requestHandlersByChannel.clear();
        readyListeners.clear();
    }

    private void removeEventListener(String channel, GrapheneBridgeEventListener listener) {
        CopyOnWriteArrayList<GrapheneBridgeEventListener> listeners = eventListenersByChannel.get(channel);
        if (listeners == null) {
            return;
        }

        listeners.remove(listener);
        if (listeners.isEmpty()) {
            eventListenersByChannel.remove(channel, listeners);
        }
    }

    private void runReadyListener(Runnable listener) {
        try {
            listener.run();
        } catch (RuntimeException exception) {
            LOGGER.warn("Graphene bridge ready listener failed", exception);
        }
    }
}
