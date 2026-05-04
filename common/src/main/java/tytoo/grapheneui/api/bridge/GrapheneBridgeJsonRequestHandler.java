package tytoo.grapheneui.api.bridge;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface GrapheneBridgeJsonRequestHandler<T, U> {
    CompletableFuture<U> handle(String channel, T payload);
}
