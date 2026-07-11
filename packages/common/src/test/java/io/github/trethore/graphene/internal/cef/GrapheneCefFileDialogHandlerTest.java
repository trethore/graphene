package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;
import org.junit.jupiter.api.Test;

class GrapheneCefFileDialogHandlerTest {
  @Test
  void completesOpenDialogWithAbsoluteSelections() {
    RecordingCallback callback = new RecordingCallback();
    GrapheneCefFileDialogHandler handler =
        new GrapheneCefFileDialogHandler(
            (foldersOnly, multiple) ->
                java.util.concurrent.CompletableFuture.completedFuture(
                    List.of(Path.of("file.txt"))),
            GrapheneTaskExecutor.direct());

    boolean handled =
        handler.onFileDialog(
            null,
            CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
            "Open",
            "",
            new Vector<>(),
            new Vector<>(),
            new Vector<>(),
            callback);

    assertTrue(handled);
    assertFalse(callback.cancelled);
    assertEquals(List.of(Path.of("file.txt").toAbsolutePath().toString()), callback.paths);
  }

  @Test
  void cancelsWhenPresenterFailsAndDelegatesSaveDialogs() {
    RecordingCallback callback = new RecordingCallback();
    GrapheneCefFileDialogHandler handler =
        new GrapheneCefFileDialogHandler(
            (foldersOnly, multiple) ->
                java.util.concurrent.CompletableFuture.failedFuture(
                    new IllegalStateException("failed")),
            GrapheneTaskExecutor.direct());

    assertTrue(
        handler.onFileDialog(
            null,
            CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN_FOLDER,
            "Folder",
            "",
            new Vector<>(),
            new Vector<>(),
            new Vector<>(),
            callback));
    assertTrue(callback.cancelled);
    assertFalse(
        handler.onFileDialog(
            null,
            CefDialogHandler.FileDialogMode.FILE_DIALOG_SAVE,
            "Save",
            "",
            new Vector<>(),
            new Vector<>(),
            new Vector<>(),
            callback));
  }

  private static final class RecordingCallback implements CefFileDialogCallback {
    private List<String> paths = List.of();
    private boolean cancelled;

    @Override
    public void Continue(Vector<String> filePaths) {
      paths = List.copyOf(filePaths);
    }

    @Override
    public void Cancel() {
      cancelled = true;
    }
  }
}
