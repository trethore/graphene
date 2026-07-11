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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

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
    GrapheneStartupOverlay overlay = new GrapheneStartupOverlay();
    return new GrapheneStartupPresenter() {
      @Override
      public void update(String stage, double progress) {
        overlay.update(stage, progress);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(
            () -> {
              if (minecraft.getOverlay() == null || minecraft.getOverlay() == overlay) {
                minecraft.setOverlay(overlay);
              }
            });
      }

      @Override
      public void close() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(
            () -> {
              if (minecraft.getOverlay() == overlay) {
                minecraft.setOverlay(null);
              }
            });
      }
    };
  }

  private static GrapheneFileDialogPresenter fileDialogPresenter() {
    return (foldersOnly, multiple) ->
        CompletableFuture.supplyAsync(
            () -> {
              String selection =
                  foldersOnly
                      ? TinyFileDialogs.tinyfd_selectFolderDialog("Select folder", "")
                      : TinyFileDialogs.tinyfd_openFileDialog(
                          "Select file", "", BufferUtils.createPointerBuffer(0), "", multiple);
              if (selection == null || selection.isBlank()) {
                return List.of();
              }
              return Arrays.stream(selection.split("\\|")).map(Path::of).toList();
            });
  }

  private static GrapheneJsDialogPresenter jsDialogPresenter() {
    return (type, originUrl, message, defaultPrompt) ->
        CompletableFuture.supplyAsync(() -> showJsDialog(type, originUrl, message, defaultPrompt));
  }

  private static GrapheneJsDialogPresenter.Result showJsDialog(
      GrapheneJsDialogPresenter.DialogType type,
      String originUrl,
      String message,
      String defaultPrompt) {
    String title = originUrl == null || originUrl.isBlank() ? "Graphene" : originUrl;
    if (type == GrapheneJsDialogPresenter.DialogType.PROMPT) {
      return showPrompt(title, message, defaultPrompt);
    }
    return showMessageDialog(type, title, message);
  }

  private static GrapheneJsDialogPresenter.Result showPrompt(
      String title, String message, String defaultPrompt) {
    String response = TinyFileDialogs.tinyfd_inputBox(title, message, defaultPrompt);
    return new GrapheneJsDialogPresenter.Result(response != null, response == null ? "" : response);
  }

  private static GrapheneJsDialogPresenter.Result showMessageDialog(
      GrapheneJsDialogPresenter.DialogType type, String title, String message) {
    boolean alert = type == GrapheneJsDialogPresenter.DialogType.ALERT;
    boolean accepted =
        TinyFileDialogs.tinyfd_messageBox(
            title, message, alert ? "ok" : "yesno", alert ? "info" : "question", true);
    return new GrapheneJsDialogPresenter.Result(accepted, "");
  }
}
