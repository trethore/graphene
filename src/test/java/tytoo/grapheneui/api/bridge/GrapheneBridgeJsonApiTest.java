package tytoo.grapheneui.api.bridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class GrapheneBridgeJsonApiTest {
    @Test
    void emitJsonSerializesPayload() {
        TestBridge bridge = new TestBridge();

        bridge.emitJson("debug:event", new SumRequest(3, 4));

        assertEquals("debug:event", bridge.emittedChannel);
        JsonObject payload = JsonParser.parseString(bridge.emittedPayloadJson).getAsJsonObject();
        assertEquals(3, payload.get("a").getAsInt());
        assertEquals(4, payload.get("b").getAsInt());
    }

    @Test
    void requestJsonSerializesRequestAndParsesResponse() {
        TestBridge bridge = new TestBridge();
        bridge.nextRequestResponseJson = "{\"result\":10}";

        SumResponse response = bridge.requestJson("debug:sum", new SumRequest(6, 4), SumResponse.class).join();

        assertEquals("debug:sum", bridge.requestedChannel);
        JsonObject payload = JsonParser.parseString(bridge.requestedPayloadJson).getAsJsonObject();
        assertEquals(6, payload.get("a").getAsInt());
        assertEquals(4, payload.get("b").getAsInt());
        assertEquals(GrapheneBridge.DEFAULT_REQUEST_TIMEOUT, bridge.requestedTimeout);
        assertEquals(10, response.result());
    }

    @Test
    void onEventJsonParsesPayload() {
        TestBridge bridge = new TestBridge();
        AtomicReference<BridgeEvent> payloadRef = new AtomicReference<>();

        try (var _ = bridge.onEventJson("debug:event", BridgeEvent.class, (_, payload) -> payloadRef.set(payload))) {
            bridge.dispatchEvent("debug:event", "{\"kind\":\"ready\",\"ok\":true}");
        }

        BridgeEvent payload = payloadRef.get();
        assertNotNull(payload);
        assertEquals("ready", payload.kind());
        assertTrue(payload.ok());
    }

    @Test
    void onRequestJsonParsesRequestAndSerializesResponse() {
        TestBridge bridge = new TestBridge();
        try (var _ = bridge.onRequestJson(
                "debug:sum",
                SumRequest.class,
                (_, payload) -> CompletableFuture.completedFuture(new SumResponse(payload.a() + payload.b()))
        )) {
            String responseJson = bridge.dispatchRequest("debug:sum", "{\"a\":2,\"b\":5}").join();
            SumResponse response = GrapheneBridgeJson.fromJson(responseJson, SumResponse.class);

            assertEquals(7, response.result());
        }
    }

    @Test
    void onEventJsonFailsFastForMalformedPayload() {
        TestBridge bridge = new TestBridge();
        bridge.onEventJson("debug:event", BridgeEvent.class, (_, _) -> {
        });

        assertThrows(IllegalArgumentException.class, () -> bridge.dispatchEvent("debug:event", "{"));
    }

    private record SumRequest(int a, int b) {
    }

    private record SumResponse(int result) {
    }

    private record BridgeEvent(String kind, boolean ok) {
    }

    private static final class TestBridge implements GrapheneBridge {
        private final Map<String, GrapheneBridgeEventListener> eventListenersByChannel = new HashMap<>();
        private final Map<String, GrapheneBridgeRequestHandler> requestHandlersByChannel = new HashMap<>();
        private String emittedChannel;
        private String emittedPayloadJson;
        private String requestedChannel;
        private String requestedPayloadJson;
        private Duration requestedTimeout;
        private String nextRequestResponseJson = "null";

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public GrapheneBridgeSubscription onReady(Runnable listener) {
            listener.run();
            return () -> {
            };
        }

        @Override
        public GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
            eventListenersByChannel.put(channel, listener);
            return () -> eventListenersByChannel.remove(channel, listener);
        }

        @Override
        public GrapheneBridgeSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler) {
            requestHandlersByChannel.put(channel, handler);
            return () -> requestHandlersByChannel.remove(channel, handler);
        }

        @Override
        public void emit(String channel, String payloadJson) {
            emittedChannel = channel;
            emittedPayloadJson = payloadJson;
        }

        @Override
        public CompletableFuture<String> request(String channel, String payloadJson, Duration timeout) {
            requestedChannel = channel;
            requestedPayloadJson = payloadJson;
            requestedTimeout = timeout;
            return CompletableFuture.completedFuture(nextRequestResponseJson);
        }

        private void dispatchEvent(String channel, String payloadJson) {
            GrapheneBridgeEventListener listener = eventListenersByChannel.get(channel);
            if (listener != null) {
                listener.onEvent(channel, payloadJson);
            }
        }

        private CompletableFuture<String> dispatchRequest(String channel, String payloadJson) {
            GrapheneBridgeRequestHandler requestHandler = requestHandlersByChannel.get(channel);
            if (requestHandler == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Missing handler for " + channel));
            }

            return requestHandler.handle(channel, payloadJson);
        }
    }
}
