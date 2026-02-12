package tytoo.grapheneui.internal.bridge;

import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridgeEventListener;
import tytoo.grapheneui.api.bridge.GrapheneBridgeRequestHandler;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

final class GrapheneBridgeHandlerRegistry {
    private final Map<String, CopyOnWriteArrayList<GrapheneBridgeEventListener>> eventListenersByChannel = new ConcurrentHashMap<>();
    private final Map<String, GrapheneBridgeRequestHandler> requestHandlersByChannel = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Runnable> readyListeners = new CopyOnWriteArrayList<>();

    GrapheneBridgeSubscription onReady(Runnable listener, boolean ready) {
        readyListeners.add(listener);
        if (ready) {
            listener.run();
        }

        return () -> readyListeners.remove(listener);
    }

    GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
        CopyOnWriteArrayList<GrapheneBridgeEventListener> listeners = eventListenersByChannel.computeIfAbsent(
                channel,
                _ -> new CopyOnWriteArrayList<>()
        );
        listeners.add(listener);
        return () -> removeEventListener(channel, listener);
    }

    GrapheneBridgeSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler) {
        requestHandlersByChannel.put(channel, handler);
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
                GrapheneCore.LOGGER.warn("Graphene bridge event listener failed for channel {}", channel, exception);
            }
        }
    }

    void notifyReady() {
        for (Runnable listener : readyListeners) {
            try {
                listener.run();
            } catch (RuntimeException exception) {
                GrapheneCore.LOGGER.warn("Graphene bridge ready listener failed", exception);
            }
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
}
