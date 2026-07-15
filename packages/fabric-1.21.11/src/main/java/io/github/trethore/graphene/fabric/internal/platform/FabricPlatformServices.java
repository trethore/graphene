package io.github.trethore.graphene.fabric.internal.platform;

import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.browser.dialog.BrowserJsDialogPresenter;
import io.github.trethore.graphene.fabric.internal.util.MinecraftReferences;
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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class FabricPlatformServices {
  private FabricPlatformServices() {}

  public static GraphenePlatformServices create() {
    return new GraphenePlatformServices(
        lifecycle(),
        mainThreadExecutor(),
        modResolver(),
        () -> GlfwNativeWindowHandle.resolve(MinecraftReferences.windowHandle()),
        windowMetrics(),
        MinecraftReferences::openUri,
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
        MinecraftReferences.execute(action);
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
        return MinecraftReferences.windowWidth();
      }

      @Override
      public int height() {
        return MinecraftReferences.windowHeight();
      }

      @Override
      public double scaleFactor() {
        return MinecraftReferences.guiScale();
      }
    };
  }

  private static GrapheneStartupPresenter startupPresenter() {
    GrapheneStartupOverlay overlay = new GrapheneStartupOverlay();
    return new GrapheneStartupPresenter() {
      @Override
      public void update(String stage, double progress) {
        overlay.update(stage, progress);
        MinecraftReferences.execute(
            () -> {
              if (MinecraftReferences.overlay() == null
                  || MinecraftReferences.overlay() == overlay) {
                MinecraftReferences.setOverlay(overlay);
              }
            });
      }

      @Override
      public void close() {
        MinecraftReferences.execute(
            () -> {
              if (MinecraftReferences.overlay() == overlay) {
                MinecraftReferences.setOverlay(null);
              }
            });
      }
    };
  }

  private static BrowserFileDialogPresenter fileDialogPresenter() {
    return request ->
        CompletableFuture.supplyAsync(
            () -> {
              String selection = showFileDialog(request);
              if (selection == null || selection.isBlank()) {
                return List.of();
              }
              return Arrays.stream(selection.split("\\|")).map(Path::of).toList();
            });
  }

  private static String showFileDialog(BrowserFileDialogPresenter.Request request) {
    String title = fileDialogTitle(request);
    return switch (request.mode()) {
      case OPEN_FOLDER ->
          TinyFileDialogs.tinyfd_selectFolderDialog(title, request.defaultFilePath());
      case OPEN_FILE, OPEN_MULTIPLE_FILES, SAVE_FILE -> showFileSelectionDialog(request, title);
    };
  }

  private static String showFileSelectionDialog(
      BrowserFileDialogPresenter.Request request, String title) {
    TinyFdFileDialogFilter filter = TinyFdFileDialogFilter.from(request.filters());
    try (MemoryStack stack = MemoryStack.stackPush()) {
      List<String> filterPatterns = filter.patterns();
      PointerBuffer patterns = null;
      if (!filterPatterns.isEmpty()) {
        patterns = stack.mallocPointer(filterPatterns.size());
        for (String pattern : filterPatterns) {
          patterns.put(stack.UTF8(pattern));
        }
        patterns.flip();
      }
      if (request.mode() == BrowserFileDialogPresenter.Mode.SAVE_FILE) {
        return TinyFileDialogs.tinyfd_saveFileDialog(
            title, request.defaultFilePath(), patterns, filter.description());
      }
      return TinyFileDialogs.tinyfd_openFileDialog(
          title,
          request.defaultFilePath(),
          patterns,
          filter.description(),
          request.mode() == BrowserFileDialogPresenter.Mode.OPEN_MULTIPLE_FILES);
    }
  }

  private static String fileDialogTitle(BrowserFileDialogPresenter.Request request) {
    if (!request.title().isBlank()) {
      return request.title();
    }
    return switch (request.mode()) {
      case OPEN_FILE, OPEN_MULTIPLE_FILES -> "Select file";
      case OPEN_FOLDER -> "Select folder";
      case SAVE_FILE -> "Save file";
    };
  }

  private static BrowserJsDialogPresenter jsDialogPresenter() {
    return request -> CompletableFuture.supplyAsync(() -> showJsDialog(request));
  }

  private static BrowserJsDialogPresenter.Result showJsDialog(
      BrowserJsDialogPresenter.Request request) {
    String title = request.originUrl().isBlank() ? "Graphene" : request.originUrl();
    if (request.type() == BrowserJsDialogPresenter.Type.PROMPT) {
      return showPrompt(title, request.message(), request.defaultPrompt());
    }
    return showMessageDialog(request.type(), title, request.message());
  }

  private static BrowserJsDialogPresenter.Result showPrompt(
      String title, String message, String defaultPrompt) {
    String response = TinyFileDialogs.tinyfd_inputBox(title, message, defaultPrompt);
    return response == null
        ? BrowserJsDialogPresenter.Result.cancel()
        : BrowserJsDialogPresenter.Result.accept(response);
  }

  private static BrowserJsDialogPresenter.Result showMessageDialog(
      BrowserJsDialogPresenter.Type type, String title, String message) {
    boolean alert = type == BrowserJsDialogPresenter.Type.ALERT;
    boolean accepted =
        TinyFileDialogs.tinyfd_messageBox(
            title, message, alert ? "ok" : "yesno", alert ? "info" : "question", true);
    return accepted
        ? BrowserJsDialogPresenter.Result.accept()
        : BrowserJsDialogPresenter.Result.cancel();
  }
}
