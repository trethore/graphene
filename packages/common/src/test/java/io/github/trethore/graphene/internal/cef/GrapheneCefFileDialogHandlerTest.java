package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;
import org.junit.jupiter.api.Test;

class GrapheneCefFileDialogHandlerTest {
  @Test
  void delegatesWhenCallbackIsMissing() {
    GrapheneCefFileDialogHandler handler =
        new GrapheneCefFileDialogHandler(
            request -> CompletableFuture.completedFuture(List.of()), GrapheneTaskExecutor.direct());

    assertFalse(
        handler.onFileDialog(
            null,
            CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
            "",
            "",
            new Vector<>(),
            new Vector<>(),
            new Vector<>(),
            null));
  }

  @Test
  void completesOpenDialogWithAbsoluteSelections() {
    RecordingCallback callback = new RecordingCallback();
    GrapheneCefFileDialogHandler handler =
        new GrapheneCefFileDialogHandler(
            request -> {
              assertEquals(BrowserFileDialogPresenter.Mode.OPEN_FILE, request.mode());
              assertEquals("Open", request.title());
              assertEquals("default.txt", request.defaultFilePath());
              assertEquals(
                  List.of(
                      new BrowserFileDialogPresenter.Filter(
                          "text/plain", ".txt;.text", "Text files")),
                  request.filters());
              return CompletableFuture.completedFuture(List.of(Path.of("file.txt")));
            },
            GrapheneTaskExecutor.direct());

    boolean handled =
        handler.onFileDialog(
            null,
            CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
            "Open",
            "default.txt",
            new Vector<>(List.of("text/plain")),
            new Vector<>(List.of(".txt;.text")),
            new Vector<>(List.of("Text files")),
            callback);

    assertTrue(handled);
    assertFalse(callback.cancelled);
    assertEquals(List.of(Path.of("file.txt").toAbsolutePath().toString()), callback.paths);
  }

  @Test
  void cancelsWhenPresenterFailsAndHandlesSaveDialogs() {
    RecordingCallback callback = new RecordingCallback();
    GrapheneCefFileDialogHandler handler =
        new GrapheneCefFileDialogHandler(
            request -> {
              throw new IllegalStateException("failed");
            },
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

    RecordingCallback saveCallback = new RecordingCallback();
    GrapheneCefFileDialogHandler saveHandler =
        new GrapheneCefFileDialogHandler(
            request -> {
              assertEquals(BrowserFileDialogPresenter.Mode.SAVE_FILE, request.mode());
              return CompletableFuture.completedFuture(List.of(Path.of("saved.txt")));
            },
            GrapheneTaskExecutor.direct());

    assertTrue(
        saveHandler.onFileDialog(
            null,
            CefDialogHandler.FileDialogMode.FILE_DIALOG_SAVE,
            "Save",
            "saved.txt",
            new Vector<>(),
            new Vector<>(),
            new Vector<>(),
            saveCallback));
    assertFalse(saveCallback.cancelled);
    assertEquals(List.of(Path.of("saved.txt").toAbsolutePath().toString()), saveCallback.paths);
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
