package tytoo.grapheneui.internal.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@FunctionalInterface
public interface GrapheneMainThreadExecutor {
    GrapheneMainThreadExecutor DIRECT = Runnable::run;

    void run(Runnable action);

    default <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        CompletableFuture<T> future = new CompletableFuture<>();
        run(() -> {
            try {
                future.complete(supplier.get());
            } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }
}
