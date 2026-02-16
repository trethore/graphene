package tytoo.grapheneui.internal.bridge;

import tytoo.grapheneui.api.bridge.GrapheneBridgeRequestException;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

final class GrapheneBridgeRequestLifecycle {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneBridgeRequestLifecycle.class);

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
        DEBUG_LOGGER.debugIfEnabled(logger -> {
            int payloadSize = payloadJson == null ? 0 : payloadJson.length();
            logger.debug(
                    "Registered bridge pending request id={} channel={} timeoutMs={} payloadSize={}",
                    requestId,
                    channel,
                    timeout.toMillis(),
                    payloadSize
            );
        });

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
            DEBUG_LOGGER.debug("Failed to enqueue bridge request id={} channel={} reason={}", requestId, channel, exception.getMessage());
        }

        return responseFuture;
    }

    void failAllForPageChange() {
        DEBUG_LOGGER.debug("Failing all pending bridge requests because page changed");
        pendingRequests.failAll(new IllegalStateException("Bridge page changed before a response was received"));
    }

    void failAllForClose() {
        DEBUG_LOGGER.debug("Failing all pending bridge requests because bridge closed");
        pendingRequests.failAll(new IllegalStateException("Bridge closed"));
    }

    void handleResponse(GrapheneBridgePacket packet) {
        if (packet.id == null || packet.id.isBlank()) {
            DEBUG_LOGGER.debug("Ignoring bridge response packet without request id");
            return;
        }

        if (Boolean.FALSE.equals(packet.ok)) {
            String errorCode = packet.error == null || packet.error.code == null ? "bridge_error" : packet.error.code;
            String errorMessage = packet.error == null || packet.error.message == null ? "Bridge request failed" : packet.error.message;
            pendingRequests.completeFailure(packet.id, new GrapheneBridgeRequestException(errorCode, errorMessage, packet.id, packet.channel));
            DEBUG_LOGGER.debug("Completed bridge request as failure id={} channel={} code={}", packet.id, packet.channel, errorCode);
            return;
        }

        pendingRequests.completeSuccess(packet.id, codec.payloadToJson(packet.payload));
        DEBUG_LOGGER.debug("Completed bridge request as success id={} channel={}", packet.id, packet.channel);
    }
}
