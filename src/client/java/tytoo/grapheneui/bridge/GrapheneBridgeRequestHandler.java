package tytoo.grapheneui.bridge;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface GrapheneBridgeRequestHandler {
    CompletableFuture<String> handle(String channel, String payloadJson);
}
