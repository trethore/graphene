package tytoo.grapheneui.bridge;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public interface GrapheneBridge {
    Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    boolean isReady();

    GrapheneBridgeSubscription onReady(Runnable listener);

    GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener);

    GrapheneBridgeSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler);

    default <T> GrapheneBridgeSubscription onEventJson(
            String channel,
            Class<T> payloadType,
            GrapheneBridgeJsonEventListener<T> listener
    ) {
        Objects.requireNonNull(payloadType, "payloadType");
        Objects.requireNonNull(listener, "listener");
        return onEvent(channel, (receivedChannel, payloadJson) ->
                listener.onEvent(receivedChannel, GrapheneBridgeJson.fromJson(payloadJson, payloadType))
        );
    }

    default <T, U> GrapheneBridgeSubscription onRequestJson(
            String channel,
            Class<T> requestType,
            GrapheneBridgeJsonRequestHandler<T, U> handler
    ) {
        Objects.requireNonNull(requestType, "requestType");
        Objects.requireNonNull(handler, "handler");
        return onRequest(channel, (requestChannel, payloadJson) -> {
            T requestPayload = GrapheneBridgeJson.fromJson(payloadJson, requestType);
            CompletableFuture<U> responseFuture = handler.handle(requestChannel, requestPayload);
            if (responseFuture == null) {
                return null;
            }

            return responseFuture.thenApply(GrapheneBridgeJson::toJson);
        });
    }

    void emit(String channel, String payloadJson);

    default void emitJson(String channel, Object payload) {
        emit(channel, GrapheneBridgeJson.toJson(payload));
    }

    default CompletableFuture<String> request(String channel, String payloadJson) {
        return request(channel, payloadJson, DEFAULT_REQUEST_TIMEOUT);
    }

    default <T> CompletableFuture<T> requestJson(String channel, Object payload, Class<T> responseType) {
        return requestJson(channel, payload, DEFAULT_REQUEST_TIMEOUT, responseType);
    }

    default <T> CompletableFuture<T> requestJson(
            String channel,
            Object payload,
            Duration timeout,
            Class<T> responseType
    ) {
        Objects.requireNonNull(responseType, "responseType");
        return request(channel, GrapheneBridgeJson.toJson(payload), timeout)
                .thenApply(responseJson -> GrapheneBridgeJson.fromJson(responseJson, responseType));
    }

    CompletableFuture<String> request(String channel, String payloadJson, Duration timeout);
}
