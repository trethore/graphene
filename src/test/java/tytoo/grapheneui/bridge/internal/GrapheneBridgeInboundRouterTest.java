package tytoo.grapheneui.bridge.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cef.callback.CefQueryCallback;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class GrapheneBridgeInboundRouterTest {
    @Test
    void routesReadyAndInvalidVersionMessages() {
        RouterFixture fixture = new RouterFixture();

        CapturingCallback readyCallback = new CapturingCallback();
        boolean readyHandled = fixture.router.route("{\"bridge\":\"graphene-ui\",\"version\":1,\"kind\":\"ready\"}", readyCallback);
        assertTrue(readyHandled);
        assertEquals("{}", readyCallback.successResponse);
        assertTrue(fixture.readySignal.get());

        CapturingCallback invalidVersionCallback = new CapturingCallback();
        boolean invalidHandled = fixture.router.route("{\"bridge\":\"graphene-ui\",\"version\":99,\"kind\":\"ready\"}", invalidVersionCallback);
        assertTrue(invalidHandled);
        assertEquals(422, invalidVersionCallback.failureCode);
        assertNotNull(invalidVersionCallback.failureMessage);
    }

    @Test
    void routesEventToRegisteredListener() {
        RouterFixture fixture = new RouterFixture();
        AtomicReference<String> payloadCapture = new AtomicReference<>();

        try (var _ = fixture.handlers.onEvent(
                "debug:event",
                (_, payloadJson) -> payloadCapture.set(payloadJson)
        )) {
            CapturingCallback callback = new CapturingCallback();
            boolean handled = fixture.router.route(
                    """
                            {
                              "bridge":"graphene-ui",
                              "version":1,
                              "kind":"event",
                              "channel":"debug:event",
                              "payload":{"value":7}
                            }
                            """,
                    callback
            );

            assertTrue(handled);
            assertEquals("{}", callback.successResponse);
            JsonObject payload = JsonParser.parseString(payloadCapture.get()).getAsJsonObject();
            assertEquals(7, payload.get("value").getAsInt());
        }
    }

    @Test
    void routesRequestThroughHandler() {
        RouterFixture fixture = new RouterFixture();

        try (var _ = fixture.handlers.onRequest("debug:sum", (_, payloadJson) -> {
            JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
            int left = payload.get("a").getAsInt();
            int right = payload.get("b").getAsInt();
            JsonObject response = new JsonObject();
            response.addProperty("result", left + right);
            return CompletableFuture.completedFuture(response.toString());
        })) {
            CapturingCallback callback = new CapturingCallback();
            boolean handled = fixture.router.route(
                    """
                            {
                              "bridge":"graphene-ui",
                              "version":1,
                              "kind":"request",
                              "id":"js-1",
                              "channel":"debug:sum",
                              "payload":{"a":4,"b":6}
                            }
                            """,
                    callback
            );

            assertTrue(handled);
            JsonObject response = JsonParser.parseString(callback.successResponse).getAsJsonObject();
            assertTrue(response.get("ok").getAsBoolean());
            assertEquals(10, response.getAsJsonObject("payload").get("result").getAsInt());
        }
    }

    @Test
    void routesResponseToPendingRequest() {
        RouterFixture fixture = new RouterFixture();
        fixture.outboundQueue.markReadyAndFlush();

        CompletableFuture<String> responseFuture = fixture.requestLifecycle.request(
                "debug:request",
                "{\"query\":true}",
                Duration.ofSeconds(1)
        );
        assertEquals(1, fixture.dispatchedOutboundMessages.size());

        JsonObject outboundRequest = JsonParser.parseString(fixture.dispatchedOutboundMessages.getFirst()).getAsJsonObject();
        String requestId = outboundRequest.get("id").getAsString();
        String responseJson = """
                {
                  "bridge":"graphene-ui",
                  "version":1,
                  "kind":"response",
                  "id":"%s",
                  "channel":"debug:request",
                  "ok":true,
                  "payload":{"done":true}
                }
                """.formatted(requestId);

        CapturingCallback callback = new CapturingCallback();
        boolean handled = fixture.router.route(responseJson, callback);

        assertTrue(handled);
        assertEquals("{}", callback.successResponse);
        JsonObject payload = JsonParser.parseString(responseFuture.join()).getAsJsonObject();
        assertTrue(payload.get("done").getAsBoolean());
    }

    private static final class RouterFixture {
        private final GrapheneBridgeMessageCodec codec = new GrapheneBridgeMessageCodec();
        private final GrapheneBridgeHandlerRegistry handlers = new GrapheneBridgeHandlerRegistry();
        private final List<String> dispatchedOutboundMessages = new ArrayList<>();
        private final GrapheneBridgeOutboundQueue outboundQueue = new GrapheneBridgeOutboundQueue(dispatchedOutboundMessages::add);
        private final GrapheneBridgeRequestLifecycle requestLifecycle = new GrapheneBridgeRequestLifecycle(codec, outboundQueue);
        private final AtomicBoolean readySignal = new AtomicBoolean(false);
        private final GrapheneBridgeInboundRouter router = new GrapheneBridgeInboundRouter(
                codec,
                handlers,
                requestLifecycle,
                () -> readySignal.set(true)
        );
    }

    private static final class CapturingCallback implements CefQueryCallback {
        private String successResponse;
        private Integer failureCode;
        private String failureMessage;

        @Override
        public void success(String response) {
            this.successResponse = response;
        }

        @Override
        public void failure(int errorCode, String errorMessage) {
            this.failureCode = errorCode;
            this.failureMessage = errorMessage;
        }
    }
}
