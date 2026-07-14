package io.github.trethore.graphene.internal.platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface GrapheneTaskExecutor {
  void execute(Runnable action);

  <T> CompletableFuture<T> supply(Supplier<T> action);

  default <T> CompletableFuture<T> supplyStage(Supplier<CompletionStage<T>> action) {
    return supply(action).thenCompose(stage -> stage);
  }

  static GrapheneTaskExecutor direct() {
    return new GrapheneTaskExecutor() {
      @Override
      public void execute(Runnable action) {
        action.run();
      }

      @Override
      public <T> CompletableFuture<T> supply(Supplier<T> action) {
        try {
          return CompletableFuture.completedFuture(action.get());
        } catch (RuntimeException exception) {
          return CompletableFuture.failedFuture(exception);
        }
      }
    };
  }
}
