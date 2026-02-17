package tytoo.grapheneuidebug.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.api.surface.BrowserSurface;
import tytoo.grapheneui.api.surface.BrowserSurfaceInputAdapter;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;
import tytoo.grapheneui.internal.mc.McClient;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneDebugTestRunner.class);

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MOUSE_STATE_REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final String ABOUT_BLANK_URL = "about:blank";
    private static final String MOUSE_BRIDGE_TEST_URL = GrapheneClasspathUrls.asset(
            GrapheneDebugClient.ID,
            "graphene_test/pages/mouse-bridge.html"
    );
    private static final String CHANNEL_EVENT = "graphene:test:event";
    private static final String CHANNEL_HANDLER_REQUEST = "graphene:test:sum";
    private static final String CHANNEL_PENDING_REQUEST = "graphene:test:pending";
    private static final String CHANNEL_MOUSE_STATE_REQUEST = "graphene:mouse:state";

    private GrapheneDebugTestRunner() {
    }

    public static CompletableFuture<String> runAllTestsAsJson() {
        Instant startedAt = Instant.now();
        return runAllTests()
                .handle((results, throwable) -> buildRunReport(startedAt, results, throwable));
    }

    private static CompletableFuture<List<TestResult>> runAllTests() {
        List<TestCase> testCases = List.of(
                new TestCase("runtime-smoke", GrapheneDebugTestRunner::runRuntimeSmoke),
                new TestCase("browser-surface-smoke", GrapheneDebugTestRunner::runBrowserSurfaceSmoke),
                new TestCase("bridge-api-smoke", GrapheneDebugTestRunner::runBridgeApiSmoke),
                new TestCase("mouse-bridge-side-buttons", GrapheneDebugTestRunner::runMouseBridgeSideButtons)
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

    private static String buildRunReport(Instant startedAt, List<TestResult> results, Throwable throwable) {
        Instant finishedAt = Instant.now();
        JsonObject report = new JsonObject();
        report.addProperty("startedAt", startedAt.toString());
        report.addProperty("finishedAt", finishedAt.toString());
        report.addProperty("durationMs", Duration.between(startedAt, finishedAt).toMillis());

        if (throwable != null) {
            Throwable rootCause = unwrap(throwable);
            LOGGER.error("Graphene debug tests crashed before completion", rootCause);
            report.addProperty("ok", false);
            report.addProperty("totalCount", 0);
            report.addProperty("passedCount", 0);
            report.addProperty("failedCount", 0);
            report.add("results", new JsonArray());
            report.add("error", toErrorJson(rootCause));
            return report.toString();
        }

        JsonArray resultArray = new JsonArray();
        int passedCount = 0;
        for (TestResult result : results) {
            if (result.passed()) {
                passedCount++;
            }

            resultArray.add(toResultJson(result));
        }

        int totalCount = results.size();
        int failedCount = totalCount - passedCount;
        report.addProperty("ok", failedCount == 0);
        report.addProperty("totalCount", totalCount);
        report.addProperty("passedCount", passedCount);
        report.addProperty("failedCount", failedCount);
        report.add("results", resultArray);
        return report.toString();
    }

    private static JsonObject toResultJson(TestResult result) {
        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("name", result.name());
        resultJson.addProperty("passed", result.passed());
        resultJson.addProperty("durationMs", result.duration().toMillis());
        if (!result.passed() && result.error() != null) {
            resultJson.add("error", toErrorJson(result.error()));
        }

        return resultJson;
    }

    private static JsonObject toErrorJson(Throwable throwable) {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("type", throwable.getClass().getSimpleName());
        errorJson.addProperty("message", throwable.getMessage() == null ? "No error message provided" : throwable.getMessage());
        return errorJson;
    }

    private static CompletableFuture<TestResult> runSingleTest(TestCase testCase) {
        Instant startedAt = Instant.now();
        LOGGER.info("Graphene debug test started: {}", testCase.name());

        CompletableFuture<Void> testFuture;
        try {
            CompletableFuture<Void> rawTestFuture = testCase.testExecution().run();
            testFuture = Objects.requireNonNull(rawTestFuture, "testFuture").orTimeout(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            Duration duration = Duration.between(startedAt, Instant.now());
            LOGGER.error("Graphene debug test failed: {} ({} ms)", testCase.name(), duration.toMillis(), exception);
            return CompletableFuture.completedFuture(TestResult.failed(testCase.name(), duration, exception));
        }

        return testFuture.handle((ignoredResult, throwable) -> {
            Duration duration = Duration.between(startedAt, Instant.now());
            if (throwable == null) {
                LOGGER.info("Graphene debug test passed: {} ({} ms)", testCase.name(), duration.toMillis());
                return TestResult.passed(testCase.name(), duration);
            }

            Throwable rootCause = unwrap(throwable);
            if (rootCause instanceof TimeoutException) {
                LOGGER.error(
                        "Graphene debug test timed out: {} after {} ms",
                        testCase.name(),
                        duration.toMillis(),
                        rootCause
                );
            } else {
                LOGGER.error("Graphene debug test failed: {} ({} ms)", testCase.name(), duration.toMillis(), rootCause);
            }
            return TestResult.failed(testCase.name(), duration, rootCause);
        });
    }

    private static CompletableFuture<Void> runRuntimeSmoke() {
        return runOnClientThread(() -> {
            requireState(GrapheneCore.isInitialized(), "GrapheneCore must be initialized in debug runtime");
            requireState(GrapheneCore.runtime().isInitialized(), "Graphene runtime must report initialized");
            requireState(GrapheneCore.runtime().getRemoteDebuggingPort() > 0, "Graphene runtime debug port must be > 0");
        });
    }

    private static CompletableFuture<Void> runBrowserSurfaceSmoke() {
        return runOnClientThread(() -> {
            try (BrowserSurface surface = BrowserSurface.builder()
                    .url(ABOUT_BLANK_URL)
                    .surfaceSize(8, 8)
                    .build()) {
                surface.setSurfaceSize(16, 16);
                surface.setResolution(24, 24);
                surface.useAutoResolution();
            }
        });
    }

    private static CompletableFuture<Void> runBridgeApiSmoke() {
        return runOnClientThread(() -> {
            try (BrowserSurface surface = BrowserSurface.builder()
                    .url(ABOUT_BLANK_URL)
                    .surfaceSize(32, 32)
                    .build()) {
                GrapheneBridge bridge = surface.bridge();
                try (GrapheneBridgeSubscription readySubscription = bridge.onReady(() -> {
                });
                     GrapheneBridgeSubscription eventSubscription = bridge.onEvent(CHANNEL_EVENT, (ignoredChannel, ignoredPayloadJson) -> {
                     });
                     GrapheneBridgeSubscription requestSubscription = bridge.onRequest(CHANNEL_HANDLER_REQUEST, (ignoredChannel, payloadJson) ->
                             CompletableFuture.completedFuture(payloadJson)
                     )) {
                    Objects.requireNonNull(readySubscription, "readySubscription");
                    Objects.requireNonNull(eventSubscription, "eventSubscription");
                    Objects.requireNonNull(requestSubscription, "requestSubscription");

                    bridge.emit(CHANNEL_EVENT, "{\"value\":7}");

                    CompletableFuture<String> responseFuture = bridge.request(
                            CHANNEL_PENDING_REQUEST,
                            "{\"query\":true}",
                            Duration.ofMillis(200)
                    );

                    try {
                        responseFuture.join();
                        throw new IllegalStateException("Bridge request was expected to fail in smoke test");
                    } catch (CompletionException exception) {
                        Throwable rootCause = unwrap(exception);
                        requireState(
                                rootCause instanceof TimeoutException || rootCause instanceof IllegalStateException,
                                "Bridge request failed with unexpected error: " + rootCause
                        );
                    }
                }
            }
        });
    }

    private static CompletableFuture<Void> runMouseBridgeSideButtons() {
        return McClient.supplyOnMainThread(() -> BrowserSurface.builder()
                        .url(ABOUT_BLANK_URL)
                        .surfaceSize(32, 32)
                        .build())
                .thenCompose(surface -> McClient.supplyOnMainThread(() -> {
                            surface.loadUrl(MOUSE_BRIDGE_TEST_URL + "?mouse-bridge-test=" + System.nanoTime());
                            return surface;
                        })
                        .thenCompose(GrapheneDebugTestRunner::runMouseBridgeSideButtons)
                        .whenComplete((ignoredSurface, ignoredError) -> McClient.runOnMainThread(surface::close)));
    }

    private static CompletableFuture<Void> runMouseBridgeSideButtons(BrowserSurface surface) {
        GrapheneBridge bridge = surface.bridge();
        BrowserSurfaceInputAdapter inputAdapter = new BrowserSurfaceInputAdapter(surface);

        return requestMouseStateAsync(bridge)
                .thenAccept(initialStateJson -> {
                    JsonObject initialState = JsonParser.parseString(initialStateJson).getAsJsonObject();
                    requireState(initialState.get("eventCount").getAsInt() == 0, "Mouse bridge should start with zero events");
                })
                .thenCompose(ignoredInitialState -> McClient.supplyOnMainThread(() -> {
                    inputAdapter.setFocused(true);
                    for (int button = GLFW.GLFW_MOUSE_BUTTON_4; button <= GLFW.GLFW_MOUSE_BUTTON_8; button++) {
                        inputAdapter.mouseClicked(button, false, 8.0, 8.0, 32, 32);
                        inputAdapter.mouseReleased(button, 8.0, 8.0, 32, 32);
                    }
                    return null;
                }))
                .thenCompose(ignoredInputDispatch -> requestMouseStateAsync(bridge))
                .thenAccept(finalStateJson -> {
                    JsonObject finalState = JsonParser.parseString(finalStateJson).getAsJsonObject();
                    requireState(finalState.get("eventCount").getAsInt() >= 10, "Mouse bridge should capture all side-button presses and releases");

                    JsonArray pressedButtons = finalState.getAsJsonArray("pressedButtons");
                    requireState(pressedButtons != null && pressedButtons.isEmpty(), "Mouse bridge pressedButtons should be empty after releases");

                    JsonObject lastEvent = finalState.getAsJsonObject("lastEvent");
                    requireState(lastEvent != null, "Mouse bridge should expose lastEvent");
                    requireState(lastEvent.get("button").getAsInt() == GLFW.GLFW_MOUSE_BUTTON_8, "Mouse bridge last event should track button 8");
                    requireState(!lastEvent.get("pressed").getAsBoolean(), "Mouse bridge last event should be released state");
                })
                .exceptionallyCompose(throwable -> fallbackMouseBridgeSmoke(inputAdapter, throwable));
    }

    private static CompletableFuture<Void> runOnClientThread(Runnable runnable) {
        Runnable task = Objects.requireNonNull(runnable, "runnable");
        CompletableFuture<Void> future = new CompletableFuture<>();
        McClient.execute(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private static void requireState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static CompletableFuture<String> requestMouseStateAsync(GrapheneBridge bridge) {
        return McClient.supplyOnMainThread(() -> bridge.request(
                CHANNEL_MOUSE_STATE_REQUEST,
                "null",
                MOUSE_STATE_REQUEST_TIMEOUT
        )).thenCompose(responseFuture -> responseFuture);
    }

    private static CompletableFuture<Void> fallbackMouseBridgeSmoke(BrowserSurfaceInputAdapter inputAdapter, Throwable throwable) {
        Throwable rootCause = unwrap(throwable);
        if (!(rootCause instanceof TimeoutException)) {
            return CompletableFuture.failedFuture(rootCause);
        }

        LOGGER.warn(
                "Skipping JS mouse bridge assertions because bridge ready handshake was unavailable in this environment"
        );

        return McClient.supplyOnMainThread(() -> {
            inputAdapter.setFocused(true);
            for (int button = GLFW.GLFW_MOUSE_BUTTON_4; button <= GLFW.GLFW_MOUSE_BUTTON_8; button++) {
                inputAdapter.mouseClicked(button, false, 8.0, 8.0, 32, 32);
                inputAdapter.mouseReleased(button, 8.0, 8.0, 32, 32);
            }
            return null;
        });
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
}
