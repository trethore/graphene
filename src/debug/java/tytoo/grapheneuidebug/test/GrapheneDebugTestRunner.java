package tytoo.grapheneuidebug.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.bridge.GrapheneBridge;
import tytoo.grapheneui.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.browser.BrowserSurface;
import tytoo.grapheneui.cef.GrapheneClasspathUrls;
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

public final class GrapheneDebugTestRunner {
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String BRIDGE_TEST_URL = GrapheneClasspathUrls.asset("graphene_test/bridge-command-test.html");
    private static final String CHANNEL_JS_HANDLER_REQUEST = "graphene:test:js-handler-request";
    private static final String CHANNEL_JAVA_HANDLER_REQUEST = "graphene:test:java-handler-request";
    private static final String CHANNEL_JAVA_EVENT = "graphene:test:java-event";
    private static final String CHANNEL_JS_ACK_EVENT = "graphene:test:js-ack-event";
    private static final String FIELD_STAGE = "stage";
    private static final String STAGE_JS_READY = "js-ready";
    private static final String STAGE_JAVA_REQUEST = "java-request";
    private static final String STAGE_JAVA_EVENT = "java-event";

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
                new TestCase("bridge-round-trip", GrapheneDebugTestRunner::runBridgeRoundTrip)
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
                surface.updateFrame();
            }
        });
    }

    private static CompletableFuture<Void> runBridgeRoundTrip() {
        CompletableFuture<Void> testFuture = new CompletableFuture<>();
        McClient.execute(() -> startBridgeRoundTrip(testFuture));
        return testFuture;
    }

    private static void startBridgeRoundTrip(CompletableFuture<Void> testFuture) {
        BridgeRoundTripContext context = null;
        try {
            BrowserSurface surface = BrowserSurface.builder()
                    .url(BRIDGE_TEST_URL)
                    .surfaceSize(32, 32)
                    .build();
            context = new BridgeRoundTripContext(surface);
            GrapheneBridge bridge = surface.bridge();

            CompletableFuture<Void> readyFuture = new CompletableFuture<>();
            CompletableFuture<String> jsReadyFuture = new CompletableFuture<>();
            CompletableFuture<String> javaRequestAckFuture = new CompletableFuture<>();
            CompletableFuture<String> javaEventAckFuture = new CompletableFuture<>();

            context.addSubscription(bridge.onReady(() -> readyFuture.complete(null)));
            context.addSubscription(bridge.onEvent(CHANNEL_JS_ACK_EVENT, (_, payloadJson) -> {
                JsonObject payload = parseJsonObject(payloadJson);
                String stage = getRequiredString(payload, FIELD_STAGE);
                if (STAGE_JAVA_REQUEST.equals(stage) && !javaRequestAckFuture.isDone()) {
                    javaRequestAckFuture.complete(payloadJson);
                    return;
                }

                if (STAGE_JS_READY.equals(stage) && !jsReadyFuture.isDone()) {
                    jsReadyFuture.complete(payloadJson);
                    return;
                }

                if (STAGE_JAVA_EVENT.equals(stage) && !javaEventAckFuture.isDone()) {
                    javaEventAckFuture.complete(payloadJson);
                }
            }));
            context.addSubscription(bridge.onRequest(CHANNEL_JAVA_HANDLER_REQUEST, (_, payloadJson) ->
                    CompletableFuture.completedFuture(buildJavaHandlerResponse(payloadJson))
            ));

            CompletableFuture<Void> flowFuture = readyFuture
                    .thenCompose(_ -> jsReadyFuture)
                    .thenCompose(_ -> bridge.request(CHANNEL_JS_HANDLER_REQUEST, "{\"probe\":\"" + STAGE_JAVA_REQUEST + "\"}"))
                    .thenAccept(GrapheneDebugTestRunner::verifyJsHandlerResponse)
                    .thenCompose(_ -> javaRequestAckFuture)
                    .thenAccept(GrapheneDebugTestRunner::verifyJavaRequestAck)
                    .thenCompose(_ -> {
                        bridge.emit(CHANNEL_JAVA_EVENT, "{\"kind\":\"java-event\"}");
                        return javaEventAckFuture;
                    })
                    .thenAccept(GrapheneDebugTestRunner::verifyJavaEventAck)
                    .thenApply(_ -> null);

            BridgeRoundTripContext finalContext = context;
            testFuture.whenComplete((_, _) -> finalContext.close());
            flowFuture.whenComplete((_, throwable) -> completeBridgeRoundTrip(testFuture, throwable));
        } catch (Exception exception) {
            if (context != null) {
                context.close();
            }
            testFuture.completeExceptionally(exception);
        }
    }

    private static String buildJavaHandlerResponse(String payloadJson) {
        JsonObject payload = parseJsonObject(payloadJson);
        String nonce = getRequiredString(payload, "nonce");
        JsonObject response = new JsonObject();
        response.addProperty("kind", "java-handler-response");
        response.addProperty("echoNonce", nonce);
        return response.toString();
    }

    private static void verifyJsHandlerResponse(String payloadJson) {
        JsonObject payload = parseJsonObject(payloadJson);
        requireState("js-handler-response".equals(getRequiredString(payload, "kind")), "Unexpected JS handler response kind");
        requireState(STAGE_JAVA_REQUEST.equals(getRequiredString(payload, "echoProbe")), "Unexpected JS handler response payload");
    }

    private static void verifyJavaRequestAck(String payloadJson) {
        JsonObject payload = parseJsonObject(payloadJson);
        requireState(STAGE_JAVA_REQUEST.equals(getRequiredString(payload, FIELD_STAGE)), "Unexpected JS ack stage for Java request");
        requireState(payload.get("ok") != null && payload.get("ok").getAsBoolean(), "JS to Java request did not report success");
        requireState("java-handler-response".equals(getRequiredString(payload, "responseKind")), "Unexpected Java handler response kind in JS ack");
        requireState("graphene-debug-test".equals(getRequiredString(payload, "echoNonce")), "Unexpected Java handler response nonce in JS ack");
    }

    private static void verifyJavaEventAck(String payloadJson) {
        JsonObject payload = parseJsonObject(payloadJson);
        requireState(STAGE_JAVA_EVENT.equals(getRequiredString(payload, FIELD_STAGE)), "Unexpected JS ack stage for Java event");
        requireState(payload.get("ok") != null && payload.get("ok").getAsBoolean(), "Java event did not report success in JS ack");
    }

    private static void completeBridgeRoundTrip(CompletableFuture<Void> testFuture, Throwable throwable) {
        if (throwable == null) {
            testFuture.complete(null);
            return;
        }

        testFuture.completeExceptionally(unwrap(throwable));
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

    private static String getRequiredString(JsonObject payload, String key) {
        if (payload.get(key) == null || !payload.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing string field: " + key);
        }

        return payload.get(key).getAsString();
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

    private record TestCase(String name, TestExecution testExecution) {
    }

    @FunctionalInterface
    private interface TestExecution {
        CompletableFuture<Void> run();
    }

    private record TestResult(String name, boolean passed, Duration duration, Throwable error) {
        private static TestResult passed(String name, Duration duration) {
            return new TestResult(name, true, duration, null);
        }

        private static TestResult failed(String name, Duration duration, Throwable error) {
            return new TestResult(name, false, duration, error);
        }
    }

    private static final class BridgeRoundTripContext {
        private final BrowserSurface surface;
        private final List<GrapheneBridgeSubscription> subscriptions = new ArrayList<>();
        private boolean closed;

        private BridgeRoundTripContext(BrowserSurface surface) {
            this.surface = surface;
        }

        private void addSubscription(GrapheneBridgeSubscription subscription) {
            subscriptions.add(subscription);
        }

        private synchronized void close() {
            if (closed) {
                return;
            }

            closed = true;
            for (GrapheneBridgeSubscription subscription : subscriptions) {
                try {
                    subscription.unsubscribe();
                } catch (RuntimeException exception) {
                    GrapheneDebugClient.LOGGER.warn("Failed to unsubscribe bridge test listener", exception);
                }
            }
            subscriptions.clear();
            try {
                surface.close();
            } catch (RuntimeException exception) {
                GrapheneDebugClient.LOGGER.warn("Failed to close bridge test surface", exception);
            }
        }
    }
}
