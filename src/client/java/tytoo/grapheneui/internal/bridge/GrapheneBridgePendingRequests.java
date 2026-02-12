package tytoo.grapheneui.internal.bridge;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class GrapheneBridgePendingRequests {
    private final Map<String, CompletableFuture<String>> pendingById = new ConcurrentHashMap<>();

    CompletableFuture<String> register(String requestId, Duration timeout) {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        pendingById.put(requestId, responseFuture);
        responseFuture.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        responseFuture.whenComplete((_, _) -> pendingById.remove(requestId));
        return responseFuture;
    }

    void completeSuccess(String requestId, String payloadJson) {
        CompletableFuture<String> pendingFuture = pendingById.remove(requestId);
        if (pendingFuture != null) {
            pendingFuture.complete(payloadJson);
        }
    }

    void completeFailure(String requestId, Throwable throwable) {
        CompletableFuture<String> pendingFuture = pendingById.remove(requestId);
        if (pendingFuture != null) {
            pendingFuture.completeExceptionally(throwable);
        }
    }

    void failAll(Throwable throwable) {
        List<CompletableFuture<String>> pendingFutures = new ArrayList<>(pendingById.values());
        pendingById.clear();

        for (CompletableFuture<String> pendingFuture : pendingFutures) {
            pendingFuture.completeExceptionally(throwable);
        }
    }
}
