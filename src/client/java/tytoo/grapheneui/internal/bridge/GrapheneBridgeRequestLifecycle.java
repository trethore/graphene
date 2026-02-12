package tytoo.grapheneui.internal.bridge;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

final class GrapheneBridgeRequestLifecycle {
    private final GrapheneBridgeMessageCodec codec;
    private final GrapheneBridgeOutboundQueue outboundQueue;
    private final GrapheneBridgePendingRequests pendingRequests = new GrapheneBridgePendingRequests();
    private final AtomicLong requestSequence = new AtomicLong();

    GrapheneBridgeRequestLifecycle(GrapheneBridgeMessageCodec codec, GrapheneBridgeOutboundQueue outboundQueue) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.outboundQueue = Objects.requireNonNull(outboundQueue, "outboundQueue");
    }

    CompletableFuture<String> request(String channel, String payloadJson, Duration timeout) {
        String requestId = "java-" + requestSequence.incrementAndGet();
        CompletableFuture<String> responseFuture = pendingRequests.register(requestId, timeout);

        try {
            String outboundJson = codec.createOutboundPacketJson(
                    GrapheneBridgeProtocol.KIND_REQUEST,
                    requestId,
                    channel,
                    codec.parsePayloadJson(payloadJson)
            );
            outboundQueue.queueOrDispatch(outboundJson);
        } catch (RuntimeException exception) {
            pendingRequests.completeFailure(requestId, exception);
        }

        return responseFuture;
    }

    void failAllForPageChange() {
        pendingRequests.failAll(new IllegalStateException("Bridge page changed before a response was received"));
    }

    void failAllForClose() {
        pendingRequests.failAll(new IllegalStateException("Bridge closed"));
    }

    void handleResponse(GrapheneBridgePacket packet) {
        if (packet.id == null || packet.id.isBlank()) {
            return;
        }

        if (Boolean.FALSE.equals(packet.ok)) {
            String errorCode = packet.error == null || packet.error.code == null ? "bridge_error" : packet.error.code;
            String errorMessage = packet.error == null || packet.error.message == null ? "Bridge request failed" : packet.error.message;
            pendingRequests.completeFailure(packet.id, new IllegalStateException(errorCode + ": " + errorMessage));
            return;
        }

        pendingRequests.completeSuccess(packet.id, codec.payloadToJson(packet.payload));
    }
}
