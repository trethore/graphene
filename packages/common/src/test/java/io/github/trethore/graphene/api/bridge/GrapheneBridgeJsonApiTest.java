package io.github.trethore.graphene.api.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.trethore.graphene.api.GrapheneSubscription;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class GrapheneBridgeJsonApiTest {
  private static final String EVENT_CHANNEL = "debug:event";
  private static final String REQUEST_CHANNEL = "debug:sum";

  @Test
  void emitJsonSerializesPayload() {
    TestBridge bridge = new TestBridge();

    bridge.emitJson(EVENT_CHANNEL, new SumRequest(3, 4));

    assertEquals(EVENT_CHANNEL, bridge.emittedChannel);
    JsonObject payload = JsonParser.parseString(bridge.emittedPayloadJson).getAsJsonObject();
    assertEquals(3, payload.get("a").getAsInt());
    assertEquals(4, payload.get("b").getAsInt());
  }

  @Test
  void requestJsonSerializesRequestAndParsesResponse() {
    TestBridge bridge = new TestBridge();
    bridge.nextRequestResponseJson = "{\"result\":10}";

    SumResponse response =
        bridge.requestJson(REQUEST_CHANNEL, new SumRequest(6, 4), SumResponse.class).join();

    assertEquals(REQUEST_CHANNEL, bridge.requestedChannel);
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

    try (GrapheneSubscription ignoredSubscription =
        bridge.onEventJson(
            EVENT_CHANNEL,
            BridgeEvent.class,
            (ignoredChannel, payload) -> payloadRef.set(payload))) {
      bridge.dispatchEvent("{\"kind\":\"ready\",\"ok\":true}");
    }

    BridgeEvent payload = payloadRef.get();
    assertNotNull(payload);
    assertEquals("ready", payload.kind());
    assertTrue(payload.ok());
  }

  @Test
  void onRequestJsonParsesRequestAndSerializesResponse() {
    TestBridge bridge = new TestBridge();
    try (GrapheneSubscription ignoredSubscription =
        bridge.onRequestJson(
            REQUEST_CHANNEL,
            SumRequest.class,
            (ignoredChannel, payload) ->
                CompletableFuture.completedFuture(new SumResponse(payload.a() + payload.b())))) {
      String responseJson = bridge.dispatchRequest().join();
      SumResponse response = GrapheneBridgeJson.fromJson(responseJson, SumResponse.class);

      assertEquals(7, response.result());
    }
  }

  @Test
  void onEventJsonFailsFastForMalformedPayload() {
    TestBridge bridge = new TestBridge();
    try (GrapheneSubscription ignoredSubscription =
        bridge.onEventJson(
            EVENT_CHANNEL, BridgeEvent.class, (ignoredChannel, ignoredPayload) -> {})) {
      assertThrows(IllegalArgumentException.class, () -> bridge.dispatchEvent("{"));
    }
  }

  private record SumRequest(int a, int b) {}

  private record SumResponse(int result) {}

  private record BridgeEvent(String kind, boolean ok) {}

  private static final class TestBridge implements GrapheneBridge {
    private final Map<String, GrapheneBridgeEventListener> eventListenersByChannel =
        new HashMap<>();
    private final Map<String, GrapheneBridgeRequestHandler> requestHandlersByChannel =
        new HashMap<>();
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
    public GrapheneSubscription onReady(Runnable listener) {
      listener.run();
      return () -> {};
    }

    @Override
    public GrapheneSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
      eventListenersByChannel.put(channel, listener);
      return () -> eventListenersByChannel.remove(channel, listener);
    }

    @Override
    public GrapheneSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler) {
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

    private void dispatchEvent(String payloadJson) {
      GrapheneBridgeEventListener listener = eventListenersByChannel.get(EVENT_CHANNEL);
      if (listener != null) {
        listener.onEvent(EVENT_CHANNEL, payloadJson);
      }
    }

    private CompletableFuture<String> dispatchRequest() {
      GrapheneBridgeRequestHandler requestHandler = requestHandlersByChannel.get(REQUEST_CHANNEL);
      if (requestHandler == null) {
        return CompletableFuture.failedFuture(
            new IllegalStateException("Missing handler for " + REQUEST_CHANNEL));
      }

      return requestHandler.handle(REQUEST_CHANNEL, "{\"a\":2,\"b\":5}");
    }
  }
}
