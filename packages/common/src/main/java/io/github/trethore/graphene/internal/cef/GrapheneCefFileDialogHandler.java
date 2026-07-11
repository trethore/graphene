package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.platform.GrapheneFileDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;

final class GrapheneCefFileDialogHandler implements CefDialogHandler {
  private final GrapheneFileDialogPresenter presenter;
  private final GrapheneTaskExecutor mainThreadExecutor;

  GrapheneCefFileDialogHandler(
      GrapheneFileDialogPresenter presenter, GrapheneTaskExecutor mainThreadExecutor) {
    this.presenter = Objects.requireNonNull(presenter, "presenter");
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
    if (mode == FileDialogMode.FILE_DIALOG_SAVE) {
      return false;
    }
    if (callback == null) {
      return true;
    }
    boolean foldersOnly = mode == FileDialogMode.FILE_DIALOG_OPEN_FOLDER;
    boolean multiple = mode == FileDialogMode.FILE_DIALOG_OPEN_MULTIPLE;
    presenter
        .show(foldersOnly, multiple)
        .whenComplete(
            (paths, failure) ->
                mainThreadExecutor.execute(() -> complete(callback, paths, failure)));
    return true;
  }

  private static void complete(
      CefFileDialogCallback callback, List<Path> paths, Throwable failure) {
    if (failure != null || paths == null || paths.isEmpty()) {
      callback.Cancel();
      return;
    }
    Vector<String> selectedPaths = new Vector<>();
    paths.stream().map(Path::toAbsolutePath).map(Path::toString).forEach(selectedPaths::add);
    callback.Continue(selectedPaths);
  }
}
