package tytoo.grapheneui.bridge;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public interface GrapheneBridge {
    Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    boolean isReady();

    GrapheneBridgeSubscription onReady(Runnable listener);

    GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener);

    GrapheneBridgeSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler);

    void emit(String channel, String payloadJson);

    default CompletableFuture<String> request(String channel, String payloadJson) {
        return request(channel, payloadJson, DEFAULT_REQUEST_TIMEOUT);
    }

    CompletableFuture<String> request(String channel, String payloadJson, Duration timeout);
}
