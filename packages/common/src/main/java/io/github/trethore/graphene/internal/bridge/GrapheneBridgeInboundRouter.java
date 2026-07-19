package io.github.trethore.graphene.internal.bridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeRequestHandler;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneBridgeInboundRouter {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgeInboundRouter.class);

  private final GrapheneBridgeMessageCodec codec;
  private final GrapheneBridgeHandlerRegistry handlers;
  private final GrapheneBridgeRequestLifecycle requestLifecycle;
  private final GrapheneTaskExecutor taskExecutor;

  GrapheneBridgeInboundRouter(
      GrapheneBridgeMessageCodec codec,
      GrapheneBridgeHandlerRegistry handlers,
      GrapheneBridgeRequestLifecycle requestLifecycle,
      GrapheneTaskExecutor taskExecutor) {
    this.codec = Objects.requireNonNull(codec, "codec");
    this.handlers = Objects.requireNonNull(handlers, "handlers");
    this.requestLifecycle = Objects.requireNonNull(requestLifecycle, "requestLifecycle");
    this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
  }

  boolean route(String requestJson, BridgeQueryCallback callback, Runnable onReady) {
    Runnable validatedOnReady = Objects.requireNonNull(onReady, "onReady");
    GrapheneBridgePacket packet = codec.parsePacket(requestJson);
    if (packet == null) {
      if (LOGGER.isDebugEnabled()) {
        int requestSize = requestJson == null ? 0 : requestJson.length();
        LOGGER.debug(
            "Ignored inbound bridge query because packet parse failed requestSize={}", requestSize);
      }
      return false;
    }

    LOGGER.debug(
        "Routing inbound bridge packet kind={} id={} channel={} VERSION={}",
        packet.kind,
        packet.id,
        packet.channel,
        GrapheneBridgeProtocol.VERSION);

    if (packet.kind == null || packet.kind.isBlank()) {
      callback.failure(400, "Bridge message is missing kind");
      return true;
    }

    switch (packet.kind) {
      case GrapheneBridgeProtocol.KIND_READY -> {
        callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
        taskExecutor.execute(validatedOnReady);
      }
      case GrapheneBridgeProtocol.KIND_EVENT -> handleEvent(packet, callback);
      case GrapheneBridgeProtocol.KIND_REQUEST -> handleRequest(packet, callback);
      case GrapheneBridgeProtocol.KIND_RESPONSE -> {
        callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
        taskExecutor.execute(() -> requestLifecycle.handleResponse(packet));
      }
      default -> callback.failure(400, "Unknown bridge message kind: " + packet.kind);
    }

    return true;
  }

  private void handleEvent(GrapheneBridgePacket packet, BridgeQueryCallback callback) {
    if (packet.channel == null || packet.channel.isBlank()) {
      callback.failure(400, "Bridge event is missing channel");
      return;
    }

    callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
    String payloadJson = codec.payloadToJson(packet.payload);
    if (LOGGER.isDebugEnabled()) {
      int payloadSize = payloadJson == null ? 0 : payloadJson.length();
      LOGGER.debug(
          "Dispatching inbound bridge event channel={} payloadSize={}",
          packet.channel,
          payloadSize);
    }
    taskExecutor.execute(() -> handlers.dispatchEvent(packet.channel, payloadJson));
  }

  private void handleRequest(GrapheneBridgePacket packet, BridgeQueryCallback callback) {
    if (packet.id == null || packet.id.isBlank()) {
      callback.success(
          codec.createErrorResponseJson(
              null, packet.channel, "invalid_request", "Bridge request is missing id"));
      return;
    }

    if (packet.channel == null || packet.channel.isBlank()) {
      callback.success(
          codec.createErrorResponseJson(
              packet.id, null, "invalid_request", "Bridge request is missing channel"));
      return;
    }

    GrapheneBridgeRequestHandler requestHandler = handlers.requestHandler(packet.channel);
    if (requestHandler == null) {
      callback.success(
          codec.createErrorResponseJson(
              packet.id,
              packet.channel,
              "handler_not_found",
              "No Java bridge handler for channel '" + packet.channel + "'"));
      return;
    }

    String requestPayloadJson = codec.payloadToJson(packet.payload);
    if (LOGGER.isDebugEnabled()) {
      int payloadSize = requestPayloadJson == null ? 0 : requestPayloadJson.length();
      LOGGER.debug(
          "Dispatching inbound bridge request id={} channel={} payloadSize={}",
          packet.id,
          packet.channel,
          payloadSize);
    }
    taskExecutor
        .supply(() -> requestHandler.handle(packet.channel, requestPayloadJson))
        .whenComplete(
            (responseFuture, throwable) -> {
              if (throwable != null) {
                Throwable rootCause = unwrap(throwable);
                LOGGER.debug(
                    "Bridge request handler failed id={} channel={} message={}",
                    packet.id,
                    packet.channel,
                    rootCause.getMessage());
                callback.success(
                    codec.createErrorResponseJson(
                        packet.id, packet.channel, "java_handler_error", rootCause.getMessage()));
                return;
              }

              handleRequestResponse(packet, callback, responseFuture);
            });
  }

  private void handleRequestResponse(
      GrapheneBridgePacket packet,
      BridgeQueryCallback callback,
      CompletableFuture<String> responseFuture) {
    if (responseFuture == null) {
      callback.success(
          codec.createSuccessResponseJson(packet.id, packet.channel, JsonNull.INSTANCE));
      LOGGER.debug(
          "Bridge request completed with null response future id={} channel={}",
          packet.id,
          packet.channel);
      return;
    }

    responseFuture.whenComplete(
        (responsePayloadJson, throwable) -> {
          if (throwable != null) {
            Throwable rootCause = unwrap(throwable);
            LOGGER.debug(
                "Bridge request future failed id={} channel={} message={}",
                packet.id,
                packet.channel,
                rootCause.getMessage());
            callback.success(
                codec.createErrorResponseJson(
                    packet.id, packet.channel, "java_handler_error", rootCause.getMessage()));
            return;
          }

          JsonElement responsePayload;
          try {
            responsePayload = codec.parsePayloadJson(responsePayloadJson);
          } catch (IllegalArgumentException exception) {
            callback.success(
                codec.createErrorResponseJson(
                    packet.id, packet.channel, "invalid_response", exception.getMessage()));
            return;
          }

          if (LOGGER.isDebugEnabled()) {
            int payloadSize = responsePayloadJson == null ? 0 : responsePayloadJson.length();
            LOGGER.debug(
                "Bridge request completed successfully id={} channel={} payloadSize={}",
                packet.id,
                packet.channel,
                payloadSize);
          }
          callback.success(
              codec.createSuccessResponseJson(packet.id, packet.channel, responsePayload));
        });
  }

  private Throwable unwrap(Throwable throwable) {
    if (throwable instanceof CompletionException completionException
        && completionException.getCause() != null) {
      return completionException.getCause();
    }

    return throwable;
  }
}
