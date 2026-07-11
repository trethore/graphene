package io.github.trethore.graphene.internal.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.runtime.GrapheneRuntimeState;
import io.github.trethore.graphene.internal.platform.GrapheneFileDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneJsDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneLifecycle;
import io.github.trethore.graphene.internal.platform.GrapheneModResolver;
import io.github.trethore.graphene.internal.platform.GraphenePlatformServices;
import io.github.trethore.graphene.internal.platform.GrapheneStartupPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class GrapheneRuntimeControllerTest {
  @Test
  void registersStartsAndStopsThroughPlatformLifecycle() {
    TestLifecycle lifecycle = new TestLifecycle();
    GrapheneRuntimeController controller = GrapheneRuntimeController.instance();
    controller.install(platformServices(lifecycle, Set.of("alpha", "beta")));

    GrapheneContext alpha = controller.register("alpha", GrapheneConfig.defaults());
    assertSame(alpha, controller.register("alpha", GrapheneConfig.defaults()));
    assertSame(alpha, controller.context("alpha"));
    assertFalse(controller.isInitialized());

    lifecycle.started.run();
    controller.initializeAsync().toCompletableFuture().join();

    assertEquals(GrapheneRuntimeState.RUNNING, controller.state());
    assertFalse(controller.httpServer().isRunning());
    GrapheneConfig betaConfig = GrapheneConfig.defaults();
    assertThrows(IllegalStateException.class, () -> controller.register("beta", betaConfig));

    lifecycle.stopping.run();
    controller.shutdownAsync().toCompletableFuture().join();
    assertEquals(GrapheneRuntimeState.STOPPED, controller.state());
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
    GrapheneFileDialogPresenter fileDialogPresenter =
        (foldersOnly, multiple) -> CompletableFuture.completedFuture(List.<Path>of());
    GrapheneJsDialogPresenter jsDialogPresenter =
        (type, originUrl, message, defaultPrompt) ->
            CompletableFuture.completedFuture(new GrapheneJsDialogPresenter.Result(false, ""));
    return new GraphenePlatformServices(
        lifecycle,
        GrapheneTaskExecutor.direct(),
        modResolver,
        () -> 1L,
        new TestWindowMetrics(),
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
}
