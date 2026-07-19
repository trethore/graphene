package io.github.trethore.graphene.internal.bridge;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneBridgePendingRequests {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgePendingRequests.class);

  private final Map<String, CompletableFuture<String>> pendingById = new ConcurrentHashMap<>();

  CompletableFuture<String> register(String requestId, Duration timeout) {
    CompletableFuture<String> responseFuture = new CompletableFuture<>();
    pendingById.put(requestId, responseFuture);
    LOGGER.debug(
        "Registered pending bridge request id={} timeoutMs={} pendingCount={}",
        requestId,
        timeout.toMillis(),
        pendingById.size());
    responseFuture.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    responseFuture.whenComplete((ignoredResult, ignoredError) -> pendingById.remove(requestId));
    return responseFuture;
  }

  void completeSuccess(String requestId, String payloadJson) {
    CompletableFuture<String> pendingFuture = pendingById.remove(requestId);
    if (pendingFuture != null) {
      pendingFuture.complete(payloadJson);
      if (LOGGER.isDebugEnabled()) {
        int payloadSize = payloadJson == null ? 0 : payloadJson.length();
        LOGGER.debug(
            "Completed pending bridge request successfully id={} payloadSize={}",
            requestId,
            payloadSize);
      }
    }
  }

  void completeFailure(String requestId, Throwable throwable) {
    CompletableFuture<String> pendingFuture = pendingById.remove(requestId);
    if (pendingFuture != null) {
      pendingFuture.completeExceptionally(throwable);
      LOGGER.debug(
          "Completed pending bridge request as failure id={} reason={}",
          requestId,
          throwable.getMessage());
    }
  }

  void failAll(Throwable throwable) {
    List<CompletableFuture<String>> pendingFutures = new ArrayList<>(pendingById.values());
    pendingById.clear();
    LOGGER.debug(
        "Failing all pending bridge requests count={} reason={}",
        pendingFutures.size(),
        throwable.getMessage());

    for (CompletableFuture<String> pendingFuture : pendingFutures) {
      pendingFuture.completeExceptionally(throwable);
    }
  }
}
