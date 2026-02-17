package tytoo.grapheneui.internal.bridge;

import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class GrapheneBridgePendingRequests {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneBridgePendingRequests.class);

    private final Map<String, CompletableFuture<String>> pendingById = new ConcurrentHashMap<>();

    CompletableFuture<String> register(String requestId, Duration timeout) {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        pendingById.put(requestId, responseFuture);
        DEBUG_LOGGER.debug(
                "Registered pending bridge request id={} timeoutMs={} pendingCount={}",
                requestId,
                timeout.toMillis(),
                pendingById.size()
        );
        responseFuture.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        responseFuture.whenComplete((_, _) -> pendingById.remove(requestId));
        return responseFuture;
    }

    void completeSuccess(String requestId, String payloadJson) {
        CompletableFuture<String> pendingFuture = pendingById.remove(requestId);
        if (pendingFuture != null) {
            pendingFuture.complete(payloadJson);
            DEBUG_LOGGER.debugIfEnabled(logger -> {
                int payloadSize = payloadJson == null ? 0 : payloadJson.length();
                logger.debug("Completed pending bridge request successfully id={} payloadSize={}", requestId, payloadSize);
            });
        }
    }

    void completeFailure(String requestId, Throwable throwable) {
        CompletableFuture<String> pendingFuture = pendingById.remove(requestId);
        if (pendingFuture != null) {
            pendingFuture.completeExceptionally(throwable);
            DEBUG_LOGGER.debug("Completed pending bridge request as failure id={} reason={}", requestId, throwable.getMessage());
        }
    }

    void failAll(Throwable throwable) {
        List<CompletableFuture<String>> pendingFutures = new ArrayList<>(pendingById.values());
        pendingById.clear();
        DEBUG_LOGGER.debug("Failing all pending bridge requests count={} reason={}", pendingFutures.size(), throwable.getMessage());

        for (CompletableFuture<String> pendingFuture : pendingFutures) {
            pendingFuture.completeExceptionally(throwable);
        }
    }
}
