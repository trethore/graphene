package io.github.trethore.graphene.internal.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.trethore.graphene.api.Graphene;
import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserRuntimeUnavailableException;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.BrowserSessions;
import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.browser.dialog.BrowserJsDialogPresenter;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.runtime.GrapheneHttpServer;
import io.github.trethore.graphene.api.runtime.GrapheneRuntime;
import io.github.trethore.graphene.api.runtime.GrapheneRuntimeState;
import io.github.trethore.graphene.internal.platform.GrapheneLifecycle;
import io.github.trethore.graphene.internal.platform.GrapheneModResolver;
import io.github.trethore.graphene.internal.platform.GraphenePlatformServices;
import io.github.trethore.graphene.internal.platform.GrapheneStartupPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class GrapheneRuntimeControllerTest {
  @Test
  void registersStartsAndStopsThroughPlatformLifecycle() {
    TestLifecycle lifecycle = new TestLifecycle();
    GrapheneRuntimeController controller = GrapheneRuntimeController.instance();
    controller.install(platformServices(lifecycle, Set.of("alpha", "beta")));
    TestBrowserRuntime browserRuntime = new TestBrowserRuntime();
    controller.installBrowserRuntime(browserRuntime);

    GrapheneContext alpha = Graphene.register("alpha", GrapheneConfig.defaults());
    BrowserSessions browserSessions = alpha.browsers();
    assertSame(alpha, Graphene.register("alpha", GrapheneConfig.defaults()));
    assertSame(alpha, Graphene.context("alpha"));
    GrapheneRuntime runtime = Graphene.runtime();
    GrapheneHttpServer httpServer = runtime.httpServer();
    assertSame(runtime, alpha.runtime());
    assertFalse(runtime.isInitialized());
    assertFalse(httpServer instanceof AutoCloseable);
    assertSame(httpServer, runtime.httpServer());
    runtime.initialization().toCompletableFuture().complete(null);
    assertFalse(runtime.initialization().toCompletableFuture().isDone());
    assertUnavailable(browserSessions, GrapheneRuntimeState.NEW);

    lifecycle.started.run();
    browserRuntime.awaitInitializationStarted();
    try {
      assertEquals(GrapheneRuntimeState.STARTING, runtime.state());
      assertUnavailable(browserSessions, GrapheneRuntimeState.STARTING);
    } finally {
      browserRuntime.completeInitialization();
    }
    runtime.initialization().toCompletableFuture().join();

    assertEquals(GrapheneRuntimeState.RUNNING, runtime.state());
    IllegalArgumentException creationException =
        assertThrows(IllegalArgumentException.class, () -> createAndClose(browserSessions));
    assertSame(browserRuntime.creationException(), creationException);
    assertFalse(httpServer.isRunning());
    GrapheneConfig betaConfig = GrapheneConfig.defaults();
    assertThrows(IllegalStateException.class, () -> Graphene.register("beta", betaConfig));

    CompletableFuture<Void> stopping = CompletableFuture.runAsync(lifecycle.stopping);
    browserRuntime.awaitShutdownStarted();
    try {
      assertEquals(GrapheneRuntimeState.STOPPING, runtime.state());
      assertUnavailable(browserSessions, GrapheneRuntimeState.STOPPING);
    } finally {
      browserRuntime.completeShutdown();
    }
    stopping.join();
    assertEquals(GrapheneRuntimeState.STOPPED, runtime.state());
    assertUnavailable(browserSessions, GrapheneRuntimeState.STOPPED);
  }

  private static void assertUnavailable(
      BrowserSessions browserSessions, GrapheneRuntimeState expectedState) {
    BrowserRuntimeUnavailableException exception =
        assertThrows(
            BrowserRuntimeUnavailableException.class, () -> createAndClose(browserSessions));
    assertEquals(expectedState, exception.runtimeState());
  }

  private static void createAndClose(BrowserSessions browserSessions) {
    try (BrowserSession createdSession = browserSessions.create("about:blank")) {
      assertNotNull(createdSession);
    }
  }

  private static GraphenePlatformServices platformServices(
      TestLifecycle lifecycle, Set<String> loadedMods) {
    GrapheneModResolver modResolver =
        new GrapheneModResolver() {
          @Override
          public String resolveModId(Class<?> anchorClass) {
            return "alpha";
          }

          @Override
          public boolean isModLoaded(String modId) {
            return loadedMods.contains(modId);
          }
        };
    GrapheneStartupPresenter startupPresenter =
        new GrapheneStartupPresenter() {
          @Override
          public void update(String stage, double progress) {
            // Runtime tests do not exercise startup presentation.
          }

          @Override
          public void close() {
            // Runtime tests do not create startup presentation resources.
          }
        };
    BrowserFileDialogPresenter fileDialogPresenter =
        request -> CompletableFuture.completedFuture(List.<Path>of());
    BrowserJsDialogPresenter jsDialogPresenter =
        request -> CompletableFuture.completedFuture(BrowserJsDialogPresenter.Result.cancel());
    return new GraphenePlatformServices(
        lifecycle,
        GrapheneTaskExecutor.direct(),
        modResolver,
        () -> 1L,
        new TestWindowMetrics(),
        url -> {},
        startupPresenter,
        fileDialogPresenter,
        jsDialogPresenter);
  }

  private static final class TestLifecycle implements GrapheneLifecycle {
    private Runnable started;
    private Runnable stopping;

    @Override
    public void onStarted(Runnable action) {
      started = action;
    }

    @Override
    public void onStopping(Runnable action) {
      stopping = action;
    }
  }

  private static final class TestWindowMetrics
      implements io.github.trethore.graphene.internal.platform.GrapheneWindowMetrics {
    @Override
    public int width() {
      return 800;
    }

    @Override
    public int height() {
      return 600;
    }

    @Override
    public double scaleFactor() {
      return 1.0;
    }
  }

  private static final class TestBrowserRuntime implements GrapheneBrowserRuntime {
    private final CountDownLatch initializationStarted = new CountDownLatch(1);
    private final CountDownLatch allowInitialization = new CountDownLatch(1);
    private final CountDownLatch shutdownStarted = new CountDownLatch(1);
    private final CountDownLatch allowShutdown = new CountDownLatch(1);
    private final IllegalArgumentException creationException =
        new IllegalArgumentException("Browser session creation reached the installed runtime");

    @Override
    public void initialize(GrapheneGlobalConfig config) {
      initializationStarted.countDown();
      await(allowInitialization);
    }

    @Override
    public void shutdown() {
      shutdownStarted.countDown();
      await(allowShutdown);
    }

    @Override
    public OptionalInt remoteDebuggingPort() {
      return OptionalInt.empty();
    }

    @Override
    public BrowserSession createSession(String url, BrowserOptions options, int width, int height) {
      throw creationException;
    }

    private IllegalArgumentException creationException() {
      return creationException;
    }

    private void awaitInitializationStarted() {
      await(initializationStarted);
    }

    private void completeInitialization() {
      allowInitialization.countDown();
    }

    private void awaitShutdownStarted() {
      await(shutdownStarted);
    }

    private void completeShutdown() {
      allowShutdown.countDown();
    }

    private static void await(CountDownLatch latch) {
      try {
        if (!latch.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Timed out waiting for runtime test transition");
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Interrupted while waiting for runtime test transition", exception);
      }
    }
  }
}
