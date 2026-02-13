package tytoo.grapheneui.api.bridge;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface GrapheneBridgeRequestHandler {
    CompletableFuture<String> handle(String channel, String payloadJson);
}
