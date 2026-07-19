package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.config.BrowserFileAccessPolicy;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;

final class GrapheneCefFileDialogHandler implements CefDialogHandler {
  private final BrowserFileAccessPolicy fileAccessPolicy;
  private final BrowserFileDialogPresenter defaultPresenter;
  private final GrapheneTaskExecutor mainThreadExecutor;

  GrapheneCefFileDialogHandler(
      BrowserFileAccessPolicy fileAccessPolicy,
      BrowserFileDialogPresenter defaultPresenter,
      GrapheneTaskExecutor mainThreadExecutor) {
    this.fileAccessPolicy = Objects.requireNonNull(fileAccessPolicy, "fileAccessPolicy");
    this.defaultPresenter = Objects.requireNonNull(defaultPresenter, "defaultPresenter");
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
  }

  @Override
  public boolean onFileDialog(
      CefBrowser browser,
      FileDialogMode mode,
      String title,
      String defaultFilePath,
      Vector<String> acceptFilters,
      Vector<String> acceptExtensions,
      Vector<String> acceptDescriptions,
      CefFileDialogCallback callback) {
    if (callback == null) {
      return false;
    }
    if (fileAccessPolicy == BrowserFileAccessPolicy.DENY) {
      callback.Cancel();
      return true;
    }
    boolean directoryIntent =
        (mode == FileDialogMode.FILE_DIALOG_OPEN || mode == FileDialogMode.FILE_DIALOG_OPEN_FOLDER)
            && browser instanceof GrapheneCefBrowserSession session
            && session.consumeDirectoryPickerIntent();
    if (mode == FileDialogMode.FILE_DIALOG_OPEN_FOLDER && !directoryIntent) {
      // CEF displays an unhandled Chromium confirmation dialog after upload-folder selection,
      // which is unsafe with off-screen rendering.
      callback.Cancel();
      return true;
    }
    BrowserFileDialogPresenter presenter = presenter(browser);
    BrowserFileDialogPresenter.Request request =
        new BrowserFileDialogPresenter.Request(
            directoryIntent ? BrowserFileDialogPresenter.Mode.OPEN_FOLDER : mode(mode),
            Objects.requireNonNullElse(title, ""),
            Objects.requireNonNullElse(defaultFilePath, ""),
            filters(acceptFilters, acceptExtensions, acceptDescriptions));
    mainThreadExecutor
        .supplyStage(() -> presenter.show(request))
        .whenComplete(
            (paths, failure) ->
                mainThreadExecutor.execute(() -> complete(callback, paths, failure)));
    return true;
  }

  private BrowserFileDialogPresenter presenter(CefBrowser browser) {
    if (browser instanceof BrowserSession session) {
      return session.options().fileDialogPresenter().orElse(defaultPresenter);
    }
    return defaultPresenter;
  }

  private static BrowserFileDialogPresenter.Mode mode(FileDialogMode mode) {
    if (mode == null) {
      return BrowserFileDialogPresenter.Mode.OPEN_FILE;
    }
    return switch (mode) {
      case FILE_DIALOG_OPEN -> BrowserFileDialogPresenter.Mode.OPEN_FILE;
      case FILE_DIALOG_OPEN_MULTIPLE -> BrowserFileDialogPresenter.Mode.OPEN_MULTIPLE_FILES;
      case FILE_DIALOG_OPEN_FOLDER -> BrowserFileDialogPresenter.Mode.OPEN_FOLDER;
      case FILE_DIALOG_SAVE -> BrowserFileDialogPresenter.Mode.SAVE_FILE;
    };
  }

  private static List<BrowserFileDialogPresenter.Filter> filters(
      Vector<String> values, Vector<String> extensions, Vector<String> descriptions) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    List<BrowserFileDialogPresenter.Filter> filters = new ArrayList<>(values.size());
    for (int index = 0; index < values.size(); index++) {
      filters.add(
          new BrowserFileDialogPresenter.Filter(
              valueAt(values, index), valueAt(extensions, index), valueAt(descriptions, index)));
    }
    return filters;
  }

  private static String valueAt(Vector<String> values, int index) {
    if (values == null || index >= values.size()) {
      return "";
    }
    return Objects.requireNonNullElse(values.get(index), "");
  }

  private static void complete(
      CefFileDialogCallback callback, List<Path> paths, Throwable failure) {
    if (failure != null || paths == null || paths.isEmpty()) {
      callback.Cancel();
      return;
    }
    Vector<String> selectedPaths = new Vector<>();
    try {
      for (Path path : paths) {
        selectedPaths.add(path.toAbsolutePath().toString());
      }
    } catch (RuntimeException exception) {
      callback.Cancel();
      return;
    }
    callback.Continue(selectedPaths);
  }
}
