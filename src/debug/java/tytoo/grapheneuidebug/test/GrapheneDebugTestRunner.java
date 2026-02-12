package tytoo.grapheneuidebug.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.cef.callback.CefQueryCallback;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.bridge.GrapheneBridge;
import tytoo.grapheneui.bridge.internal.GrapheneBridgeEndpoint;
import tytoo.grapheneui.browser.BrowserSurface;
import tytoo.grapheneui.mc.McClient;
import tytoo.grapheneuidebug.GrapheneDebugClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class GrapheneDebugTestRunner {
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String CHANNEL_EVENT = "graphene:test:event";
    private static final String CHANNEL_HANDLER_REQUEST = "graphene:test:sum";
    private static final String CHANNEL_PENDING_REQUEST = "graphene:test:pending";

    private GrapheneDebugTestRunner() {
    }

    public static void run(FabricClientCommandSource source) {
        runAllTests().whenComplete((results, throwable) -> {
            if (throwable != null) {
                Throwable rootCause = unwrap(throwable);
                GrapheneDebugClient.LOGGER.error("Graphene debug tests crashed before completion", rootCause);
                sendFeedback(source, Component.translatable("command.graphene-ui-debug.test.fail", 0, 0));
                return;
            }

            int passedCount = 0;
            for (TestResult result : results) {
                if (result.passed()) {
                    passedCount++;
                }
            }

            int totalCount = results.size();
            if (passedCount == totalCount) {
                sendFeedback(source, Component.translatable("command.graphene-ui-debug.test.pass", passedCount, totalCount));
                return;
            }

            sendFeedback(source, Component.translatable("command.graphene-ui-debug.test.fail", passedCount, totalCount));
        });
    }

    private static CompletableFuture<List<TestResult>> runAllTests() {
        List<TestCase> testCases = List.of(
                new TestCase("runtime-smoke", GrapheneDebugTestRunner::runRuntimeSmoke),
                new TestCase("browser-surface-smoke", GrapheneDebugTestRunner::runBrowserSurfaceSmoke),
                new TestCase("bridge-inbound-router-smoke", GrapheneDebugTestRunner::runBridgeInboundRouterSmoke)
        );

        CompletableFuture<List<TestResult>> sequenceFuture = CompletableFuture.completedFuture(new ArrayList<>());
        for (TestCase testCase : testCases) {
            sequenceFuture = sequenceFuture.thenCompose(results -> runSingleTest(testCase)
                    .thenApply(result -> {
                        results.add(result);
                        return results;
                    }));
        }

        return sequenceFuture;
    }

    private static CompletableFuture<TestResult> runSingleTest(TestCase testCase) {
        Instant startedAt = Instant.now();
        GrapheneDebugClient.LOGGER.info("Graphene debug test started: {}", testCase.name());

        CompletableFuture<Void> testFuture;
        try {
            CompletableFuture<Void> rawTestFuture = testCase.testExecution().run();
            testFuture = Objects.requireNonNull(rawTestFuture, "testFuture").orTimeout(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            Duration duration = Duration.between(startedAt, Instant.now());
            GrapheneDebugClient.LOGGER.error("Graphene debug test failed: {} ({} ms)", testCase.name(), duration.toMillis(), exception);
            return CompletableFuture.completedFuture(TestResult.failed(testCase.name(), duration, exception));
        }

        return testFuture.handle((_, throwable) -> {
            Duration duration = Duration.between(startedAt, Instant.now());
            if (throwable == null) {
                GrapheneDebugClient.LOGGER.info("Graphene debug test passed: {} ({} ms)", testCase.name(), duration.toMillis());
                return TestResult.passed(testCase.name(), duration);
            }

            Throwable rootCause = unwrap(throwable);
            if (rootCause instanceof TimeoutException) {
                GrapheneDebugClient.LOGGER.error(
                        "Graphene debug test timed out: {} after {} ms",
                        testCase.name(),
                        duration.toMillis(),
                        rootCause
                );
            } else {
                GrapheneDebugClient.LOGGER.error("Graphene debug test failed: {} ({} ms)", testCase.name(), duration.toMillis(), rootCause);
            }
            return TestResult.failed(testCase.name(), duration, rootCause);
        });
    }

    private static CompletableFuture<Void> runRuntimeSmoke() {
        return runOnClientThread(() -> {
            requireState(GrapheneCore.isInitialized(), "GrapheneCore must be initialized in debug runtime");
            requireState(GrapheneCore.runtime().isInitialized(), "Graphene runtime must report initialized");
            Objects.requireNonNull(GrapheneCore.runtime().requireClient(), "Graphene runtime CefClient must not be null");
        });
    }

    private static CompletableFuture<Void> runBrowserSurfaceSmoke() {
        return runOnClientThread(() -> {
            try (BrowserSurface surface = BrowserSurface.builder()
                    .url("about:blank")
                    .surfaceSize(8, 8)
                    .build()) {
                surface.setSurfaceSize(16, 16);
            }
        });
    }

    private static CompletableFuture<Void> runBridgeInboundRouterSmoke() {
        return runOnClientThread(() -> {
            try (BrowserSurface surface = BrowserSurface.builder()
                    .url("about:blank")
                    .surfaceSize(32, 32)
                    .build()) {
                GrapheneBridge bridge = surface.bridge();
                GrapheneBridgeEndpoint endpoint = requireBridgeEndpoint(bridge);

                assertReadyHandshake(endpoint, bridge);
                assertInvalidVersionIsRejected(endpoint);
                assertInboundEventRouting(endpoint, bridge);
                assertInboundRequestRouting(endpoint, bridge);
                assertInboundResponseRouting(endpoint, bridge);
            }
        });
    }

    private static GrapheneBridgeEndpoint requireBridgeEndpoint(GrapheneBridge bridge) {
        if (bridge instanceof GrapheneBridgeEndpoint bridgeEndpoint) {
            return bridgeEndpoint;
        }

        throw new IllegalStateException("Unexpected GrapheneBridge implementation: " + bridge.getClass().getName());
    }

    private static void assertReadyHandshake(GrapheneBridgeEndpoint endpoint, GrapheneBridge bridge) {
        AtomicBoolean readySignal = new AtomicBoolean(false);
        try (var _ = bridge.onReady(() -> readySignal.set(true))) {
            CapturingQueryCallback callback = new CapturingQueryCallback();
            boolean handled = endpoint.handleQuery(
                    "{\"bridge\":\"graphene-ui\",\"version\":1,\"kind\":\"ready\"}",
                    callback
            );

            requireState(handled, "Ready query was not handled");
            requireState("{}".equals(callback.successResponse()), "Ready query did not return success response");
            requireState(readySignal.get(), "Bridge onReady listener did not fire");
        }
    }

    private static void assertInvalidVersionIsRejected(GrapheneBridgeEndpoint endpoint) {
        CapturingQueryCallback callback = new CapturingQueryCallback();
        boolean handled = endpoint.handleQuery(
                "{\"bridge\":\"graphene-ui\",\"version\":99,\"kind\":\"ready\"}",
                callback
        );

        requireState(handled, "Invalid version query was not handled");
        requireState(Integer.valueOf(422).equals(callback.failureCode()), "Invalid version query did not return 422");
        requireState(callback.failureMessage() != null, "Invalid version query did not return an error message");
    }

    private static void assertInboundEventRouting(GrapheneBridgeEndpoint endpoint, GrapheneBridge bridge) {
        AtomicReference<String> payloadCapture = new AtomicReference<>();
        try (var _ = bridge.onEvent(CHANNEL_EVENT, (_, payloadJson) -> payloadCapture.set(payloadJson))) {
            CapturingQueryCallback callback = new CapturingQueryCallback();
            boolean handled = endpoint.handleQuery(
                    """
                            {
                              "bridge":"graphene-ui",
                              "version":1,
                              "kind":"event",
                              "channel":"graphene:test:event",
                              "payload":{"value":7}
                            }
                            """,
                    callback
            );

            requireState(handled, "Inbound event query was not handled");
            requireState("{}".equals(callback.successResponse()), "Inbound event query did not return success response");
            JsonObject payload = parseJsonObject(payloadCapture.get());
            requireState(payload.get("value") != null && payload.get("value").getAsInt() == 7, "Inbound event payload mismatch");
        }
    }

    private static void assertInboundRequestRouting(GrapheneBridgeEndpoint endpoint, GrapheneBridge bridge) {
        try (var _ = bridge.onRequest(CHANNEL_HANDLER_REQUEST, (_, payloadJson) -> {
            JsonObject payload = parseJsonObject(payloadJson);
            int left = payload.get("a").getAsInt();
            int right = payload.get("b").getAsInt();
            JsonObject response = new JsonObject();
            response.addProperty("result", left + right);
            return CompletableFuture.completedFuture(response.toString());
        })) {
            CapturingQueryCallback callback = new CapturingQueryCallback();
            boolean handled = endpoint.handleQuery(
                    """
                            {
                              "bridge":"graphene-ui",
                              "version":1,
                              "kind":"request",
                              "id":"js-1",
                              "channel":"graphene:test:sum",
                              "payload":{"a":4,"b":6}
                            }
                            """,
                    callback
            );

            requireState(handled, "Inbound request query was not handled");
            JsonObject response = parseJsonObject(callback.successResponse());
            requireState(response.get("ok") != null && response.get("ok").getAsBoolean(), "Inbound request did not return ok=true");
            requireState(
                    response.getAsJsonObject("payload").get("result").getAsInt() == 10,
                    "Inbound request did not return expected sum"
            );
        }
    }

    private static void assertInboundResponseRouting(GrapheneBridgeEndpoint endpoint, GrapheneBridge bridge) {
        CompletableFuture<String> responseFuture = bridge.request(
                CHANNEL_PENDING_REQUEST,
                "{\"query\":true}",
                Duration.ofSeconds(1)
        );

        CapturingQueryCallback callback = new CapturingQueryCallback();
        boolean handled = endpoint.handleQuery(
                """
                        {
                          "bridge":"graphene-ui",
                          "version":1,
                          "kind":"response",
                          "id":"java-1",
                          "channel":"graphene:test:pending",
                          "ok":true,
                          "payload":{"done":true}
                        }
                        """,
                callback
        );

        requireState(handled, "Inbound response query was not handled");
        requireState("{}".equals(callback.successResponse()), "Inbound response query did not return success response");
        JsonObject payload = parseJsonObject(responseFuture.join());
        requireState(payload.get("done") != null && payload.get("done").getAsBoolean(), "Inbound response payload mismatch");
    }

    private static CompletableFuture<Void> runOnClientThread(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        McClient.execute(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private static JsonObject parseJsonObject(String payloadJson) {
        try {
            return JsonParser.parseString(payloadJson).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Expected JSON object payload but got: " + payloadJson, exception);
        }
    }

    private static void requireState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void sendFeedback(FabricClientCommandSource source, Component feedback) {
        McClient.execute(() -> source.sendFeedback(feedback));
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }

        return throwable;
    }

    @FunctionalInterface
    private interface TestExecution {
        CompletableFuture<Void> run();
    }

    private record TestCase(String name, TestExecution testExecution) {
    }

    private record TestResult(String name, boolean passed, Duration duration, Throwable error) {
        private static TestResult passed(String name, Duration duration) {
            return new TestResult(name, true, duration, null);
        }

        private static TestResult failed(String name, Duration duration, Throwable error) {
            return new TestResult(name, false, duration, error);
        }
    }

    private static final class CapturingQueryCallback implements CefQueryCallback {
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

        private String successResponse() {
            return successResponse;
        }

        private Integer failureCode() {
            return failureCode;
        }

        private String failureMessage() {
            return failureMessage;
        }
    }
}
