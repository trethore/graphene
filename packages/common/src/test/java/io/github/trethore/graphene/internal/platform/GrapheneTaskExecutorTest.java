package io.github.trethore.graphene.internal.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

final class GrapheneTaskExecutorTest {
    @Test
    void suppliesValuesThroughTheConfiguredExecutor() {
        GrapheneTaskExecutor executor = Runnable::run;

        assertEquals("value", executor.supply(() -> "value").join());
    }

    @Test
    void completesExceptionallyWhenTheSupplierFails() {
        RuntimeException failure = new RuntimeException("failure");
        GrapheneTaskExecutor executor = Runnable::run;
        CompletableFuture<Object> future = executor.supply(() -> {
            throw failure;
        });

        CompletionException result =
                org.junit.jupiter.api.Assertions.assertThrows(CompletionException.class, future::join);

        assertSame(failure, result.getCause());
    }
}
