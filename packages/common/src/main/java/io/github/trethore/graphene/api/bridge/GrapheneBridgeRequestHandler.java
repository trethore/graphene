package io.github.trethore.graphene.api.bridge;

import java.util.concurrent.CompletableFuture;

/** Handles a bridge request and completes with a JSON response payload. */
@FunctionalInterface
public interface GrapheneBridgeRequestHandler {
  /**
   * Handles the request asynchronously. A {@code null} future or successful {@code null} payload
   * produces a JSON {@code null} response; thrown or asynchronous failures reject the request.
   */
  CompletableFuture<String> handle(String channel, String payloadJson);
}
