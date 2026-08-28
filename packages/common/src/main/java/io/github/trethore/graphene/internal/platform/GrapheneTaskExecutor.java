package io.github.trethore.graphene.internal.platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface GrapheneTaskExecutor {
    void execute(Runnable action);

    default <T> CompletableFuture<T> supply(Supplier<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        execute(() -> {
            try {
                future.complete(action.get());
            } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    default <T> CompletableFuture<T> supplyStage(Supplier<CompletionStage<T>> action) {
        return supply(action).thenCompose(stage -> stage);
    }

    static GrapheneTaskExecutor direct() {
        return Runnable::run;
    }
}
