package io.github.trethore.graphene.api.bridge;

import java.util.concurrent.CompletableFuture;

/** Handles a deserialized bridge request and completes with a response value. */
@FunctionalInterface
public interface GrapheneBridgeJsonRequestHandler<T, U> {
  /**
   * Handles the request asynchronously. A {@code null} future or successful {@code null} value
   * produces a JSON {@code null} response; thrown or asynchronous failures reject the request.
   */
  CompletableFuture<U> handle(String channel, T payload);
}
