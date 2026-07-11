package io.github.trethore.graphene.fabric.internal.platform;

import io.github.trethore.graphene.internal.platform.GrapheneFileDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneJsDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneLifecycle;
import io.github.trethore.graphene.internal.platform.GrapheneModResolver;
import io.github.trethore.graphene.internal.platform.GraphenePlatformServices;
import io.github.trethore.graphene.internal.platform.GrapheneStartupPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import io.github.trethore.graphene.internal.platform.GrapheneWindowMetrics;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;

public final class FabricPlatformServices {
  private FabricPlatformServices() {}

  public static GraphenePlatformServices create() {
    return new GraphenePlatformServices(
        lifecycle(),
        mainThreadExecutor(),
        modResolver(),
        () -> Minecraft.getInstance().getWindow().handle(),
        windowMetrics(),
        startupPresenter(),
        fileDialogPresenter(),
        jsDialogPresenter());
  }

  private static GrapheneLifecycle lifecycle() {
    return new GrapheneLifecycle() {
      @Override
      public void onStarted(Runnable action) {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> action.run());
      }

      @Override
      public void onStopping(Runnable action) {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> action.run());
      }
    };
  }

  private static GrapheneTaskExecutor mainThreadExecutor() {
    return new GrapheneTaskExecutor() {
      @Override
      public void execute(Runnable action) {
        Minecraft.getInstance().execute(action);
      }

      @Override
      public <T> CompletableFuture<T> supply(Supplier<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        execute(
            () -> {
              try {
                future.complete(action.get());
              } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
              }
            });
        return future;
      }
    };
  }

  private static GrapheneModResolver modResolver() {
    return new GrapheneModResolver() {
      @Override
      public String resolveModId(Class<?> anchorClass) {
        String classFilePath = anchorClass.getName().replace('.', '/') + ".class";
        String resolvedModId = null;
        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
          if (modContainer.findPath(classFilePath).isEmpty()) {
            continue;
          }
          String candidateModId = modContainer.getMetadata().getId();
          if (resolvedModId != null && !resolvedModId.equals(candidateModId)) {
            throw new IllegalStateException(
                "Anchor class belongs to multiple mod containers: "
                    + resolvedModId
                    + " and "
                    + candidateModId);
          }
          resolvedModId = candidateModId;
        }
        if (resolvedModId == null) {
          throw new IllegalArgumentException("No Fabric mod contains " + anchorClass.getName());
        }
        return resolvedModId;
      }

      @Override
      public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
      }
    };
  }

  private static GrapheneWindowMetrics windowMetrics() {
    return new GrapheneWindowMetrics() {
      @Override
      public int width() {
        return Minecraft.getInstance().getWindow().getWidth();
      }

      @Override
      public int height() {
        return Minecraft.getInstance().getWindow().getHeight();
      }

      @Override
      public double scaleFactor() {
        return Minecraft.getInstance().getWindow().getGuiScale();
      }
    };
  }

  private static GrapheneStartupPresenter startupPresenter() {
    return new GrapheneStartupPresenter() {
      @Override
      public void update(String stage, double progress) {
        // The Fabric startup overlay is introduced with the JCEF installer migration.
      }

      @Override
      public void close() {
        // No overlay exists until the JCEF installer migration creates one.
      }
    };
  }

  private static GrapheneFileDialogPresenter fileDialogPresenter() {
    return (foldersOnly, multiple) -> CompletableFuture.completedFuture(List.<Path>of());
  }

  private static GrapheneJsDialogPresenter jsDialogPresenter() {
    return (originUrl, message, defaultPrompt) ->
        CompletableFuture.completedFuture(new GrapheneJsDialogPresenter.Result(false, ""));
  }
}
