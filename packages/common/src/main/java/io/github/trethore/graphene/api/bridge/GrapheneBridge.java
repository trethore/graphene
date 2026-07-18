package io.github.trethore.graphene.api.bridge;

import io.github.trethore.graphene.api.GrapheneSubscription;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Bidirectional event and request bridge between Java and browser content. Consumer channels must
 * not begin with {@link #RESERVED_CHANNEL_PREFIX}, which is reserved for platform integrations.
 */
@SuppressWarnings("unused")
public interface GrapheneBridge {
  Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

  /** Channel prefix reserved for Graphene platform integrations. */
  String RESERVED_CHANNEL_PREFIX = "graphene:";

  /** Returns whether the bridge is exposed to the current document and ready to send messages. */
  boolean isReady();

  /** Subscribes to transitions into the ready state, invoking the listener immediately if ready. */
  GrapheneSubscription onReady(Runnable listener);

  GrapheneSubscription onEvent(String channel, GrapheneBridgeEventListener listener);

  GrapheneSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler);

  default <T> GrapheneSubscription onEventJson(
      String channel, Class<T> payloadType, GrapheneBridgeJsonEventListener<T> listener) {
    Objects.requireNonNull(payloadType, "payloadType");
    Objects.requireNonNull(listener, "listener");
    return onEvent(
        channel,
        (receivedChannel, payloadJson) ->
            listener.onEvent(
                receivedChannel, GrapheneBridgeJson.fromJson(payloadJson, payloadType)));
  }

  default <T, U> GrapheneSubscription onRequestJson(
      String channel, Class<T> requestType, GrapheneBridgeJsonRequestHandler<T, U> handler) {
    Objects.requireNonNull(requestType, "requestType");
    Objects.requireNonNull(handler, "handler");
    return onRequest(
        channel,
        (requestChannel, payloadJson) -> {
          T requestPayload = GrapheneBridgeJson.fromJson(payloadJson, requestType);
          CompletableFuture<U> responseFuture = handler.handle(requestChannel, requestPayload);
          if (responseFuture == null) {
            return null;
          }

          return responseFuture.thenApply(GrapheneBridgeJson::toJson);
        });
  }

  /** Emits an event to the current document. */
  void emit(String channel, String payloadJson);

  default void emitJson(String channel, Object payload) {
    emit(channel, GrapheneBridgeJson.toJson(payload));
  }

  default CompletableFuture<String> request(String channel, String payloadJson) {
    return request(channel, payloadJson, DEFAULT_REQUEST_TIMEOUT);
  }

  default <T> CompletableFuture<T> requestJson(
      String channel, Object payload, Class<T> responseType) {
    return requestJson(channel, payload, DEFAULT_REQUEST_TIMEOUT, responseType);
  }

  default <T> CompletableFuture<T> requestJson(
      String channel, Object payload, Duration timeout, Class<T> responseType) {
    Objects.requireNonNull(responseType, "responseType");
    return request(channel, GrapheneBridgeJson.toJson(payload), timeout)
        .thenApply(responseJson -> GrapheneBridgeJson.fromJson(responseJson, responseType));
  }

  /**
   * Sends a request and completes with its response payload.
   *
   * @throws GrapheneBridgeRequestException if the remote request fails
   */
  CompletableFuture<String> request(String channel, String payloadJson, Duration timeout);
}
