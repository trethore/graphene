package tytoo.grapheneui.internal.bridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.cef.callback.CefQueryCallback;
import tytoo.grapheneui.api.bridge.GrapheneBridgeRequestHandler;
import tytoo.grapheneui.internal.mc.McClient;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class GrapheneBridgeInboundRouter {
    private final GrapheneBridgeMessageCodec codec;
    private final GrapheneBridgeHandlerRegistry handlers;
    private final GrapheneBridgeRequestLifecycle requestLifecycle;
    private final Runnable onReady;

    GrapheneBridgeInboundRouter(
            GrapheneBridgeMessageCodec codec,
            GrapheneBridgeHandlerRegistry handlers,
            GrapheneBridgeRequestLifecycle requestLifecycle,
            Runnable onReady
    ) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.handlers = Objects.requireNonNull(handlers, "handlers");
        this.requestLifecycle = Objects.requireNonNull(requestLifecycle, "requestLifecycle");
        this.onReady = Objects.requireNonNull(onReady, "onReady");
    }

    boolean route(String requestJson, CefQueryCallback callback) {
        GrapheneBridgePacket packet = codec.parsePacket(requestJson);
        if (packet == null) {
            return false;
        }

        if (packet.version != GrapheneBridgeProtocol.VERSION) {
            callback.failure(422, "Unsupported bridge protocol version: " + packet.version);
            return true;
        }

        if (packet.kind == null || packet.kind.isBlank()) {
            callback.failure(400, "Bridge message is missing kind");
            return true;
        }

        switch (packet.kind) {
            case GrapheneBridgeProtocol.KIND_READY -> {
                callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
                McClient.runOnMainThread(onReady);
            }
            case GrapheneBridgeProtocol.KIND_EVENT -> handleEvent(packet, callback);
            case GrapheneBridgeProtocol.KIND_REQUEST -> handleRequest(packet, callback);
            case GrapheneBridgeProtocol.KIND_RESPONSE -> {
                callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
                McClient.runOnMainThread(() -> requestLifecycle.handleResponse(packet));
            }
            default -> callback.failure(400, "Unknown bridge message kind: " + packet.kind);
        }

        return true;
    }

    private void handleEvent(GrapheneBridgePacket packet, CefQueryCallback callback) {
        if (packet.channel == null || packet.channel.isBlank()) {
            callback.failure(400, "Bridge event is missing channel");
            return;
        }

        callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
        String payloadJson = codec.payloadToJson(packet.payload);
        McClient.runOnMainThread(() -> handlers.dispatchEvent(packet.channel, payloadJson));
    }

    private void handleRequest(GrapheneBridgePacket packet, CefQueryCallback callback) {
        if (packet.id == null || packet.id.isBlank()) {
            callback.success(codec.createErrorResponseJson(null, packet.channel, "invalid_request", "Bridge request is missing id"));
            return;
        }

        if (packet.channel == null || packet.channel.isBlank()) {
            callback.success(codec.createErrorResponseJson(packet.id, null, "invalid_request", "Bridge request is missing channel"));
            return;
        }

        GrapheneBridgeRequestHandler requestHandler = handlers.requestHandler(packet.channel);
        if (requestHandler == null) {
            callback.success(
                    codec.createErrorResponseJson(
                            packet.id,
                            packet.channel,
                            "handler_not_found",
                            "No Java bridge handler for channel '" + packet.channel + "'"
                    )
            );
            return;
        }

        String requestPayloadJson = codec.payloadToJson(packet.payload);
        McClient
                .supplyOnMainThread(() -> requestHandler.handle(packet.channel, requestPayloadJson))
                .whenComplete((responseFuture, throwable) -> {
                    if (throwable != null) {
                        Throwable rootCause = unwrap(throwable);
                        callback.success(codec.createErrorResponseJson(packet.id, packet.channel, "java_handler_error", rootCause.getMessage()));
                        return;
                    }

                    handleRequestResponse(packet, callback, responseFuture);
                });
    }

    private void handleRequestResponse(GrapheneBridgePacket packet, CefQueryCallback callback, CompletableFuture<String> responseFuture) {
        if (responseFuture == null) {
            callback.success(codec.createSuccessResponseJson(packet.id, packet.channel, JsonNull.INSTANCE));
            return;
        }

        responseFuture.whenComplete((responsePayloadJson, throwable) -> {
            if (throwable != null) {
                Throwable rootCause = unwrap(throwable);
                callback.success(codec.createErrorResponseJson(packet.id, packet.channel, "java_handler_error", rootCause.getMessage()));
                return;
            }

            JsonElement responsePayload;
            try {
                responsePayload = codec.parsePayloadJson(responsePayloadJson);
            } catch (IllegalArgumentException exception) {
                callback.success(codec.createErrorResponseJson(packet.id, packet.channel, "invalid_response", exception.getMessage()));
                return;
            }

            callback.success(codec.createSuccessResponseJson(packet.id, packet.channel, responsePayload));
        });
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }

        return throwable;
    }
}
