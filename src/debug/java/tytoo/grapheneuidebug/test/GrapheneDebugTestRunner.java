package tytoo.grapheneuidebug.test;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.api.surface.BrowserSurface;
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
                new TestCase("bridge-api-smoke", GrapheneDebugTestRunner::runBridgeApiSmoke)
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
            requireState(GrapheneCore.runtime().getRemoteDebuggingPort() > 0, "Graphene runtime debug port must be > 0");
        });
    }

    private static CompletableFuture<Void> runBrowserSurfaceSmoke() {
        return runOnClientThread(() -> {
            try (BrowserSurface surface = BrowserSurface.builder()
                    .url("about:blank")
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
                    .url("about:blank")
                    .surfaceSize(32, 32)
                    .build()) {
                GrapheneBridge bridge = surface.bridge();

                try (GrapheneBridgeSubscription readySubscription = bridge.onReady(() -> {
                });
                     GrapheneBridgeSubscription eventSubscription = bridge.onEvent(CHANNEL_EVENT, (_, _) -> {
                     });
                     GrapheneBridgeSubscription requestSubscription = bridge.onRequest(CHANNEL_HANDLER_REQUEST, (_, payloadJson) ->
                             CompletableFuture.completedFuture(payloadJson)
                     )) {
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

    private static CompletableFuture<Void> runOnClientThread(Runnable runnable) {
        Minecraft minecraft = Objects.requireNonNull(Minecraft.getInstance(), "Minecraft client is not available");
        CompletableFuture<Void> future = new CompletableFuture<>();
        minecraft.execute(() -> {
            try {
                runnable.run();
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

    private static void sendFeedback(FabricClientCommandSource source, Component feedback) {
        Minecraft minecraft = Objects.requireNonNull(Minecraft.getInstance(), "Minecraft client is not available");
        minecraft.execute(() -> source.sendFeedback(feedback));
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
