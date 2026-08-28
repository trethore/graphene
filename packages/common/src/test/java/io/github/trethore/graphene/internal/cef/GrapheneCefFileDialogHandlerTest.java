package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPolicy;
import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.config.BrowserFileAccessPolicy;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrapheneCefFileDialogHandlerTest {
    @Test
    void delegatesWhenCallbackIsMissing() {
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> CompletableFuture.completedFuture(List.of()),
                GrapheneTaskExecutor.direct());

        assertFalse(handler.onFileDialog(
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
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    assertEquals(BrowserFileDialogPresenter.Mode.OPEN_FILE, request.mode());
                    assertEquals("Open", request.title());
                    assertEquals("default.txt", request.defaultFilePath());
                    assertEquals(
                            List.of(new BrowserFileDialogPresenter.Filter("text/plain", ".txt;.text", "Text files")),
                            request.filters());
                    return CompletableFuture.completedFuture(List.of(Path.of("file.txt")));
                },
                GrapheneTaskExecutor.direct());

        boolean handled = handler.onFileDialog(
                browser(BrowserOptions.defaults(), false),
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
    void allowsMultipleFileDialogs() {
        RecordingCallback callback = new RecordingCallback();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    assertEquals(BrowserFileDialogPresenter.Mode.OPEN_MULTIPLE_FILES, request.mode());
                    return CompletableFuture.completedFuture(List.of(Path.of("first.txt"), Path.of("second.txt")));
                },
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                browser(BrowserOptions.defaults(), false),
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN_MULTIPLE,
                "Open",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));
        assertFalse(callback.cancelled);
        assertEquals(
                List.of(
                        Path.of("first.txt").toAbsolutePath().toString(),
                        Path.of("second.txt").toAbsolutePath().toString()),
                callback.paths);
    }

    @Test
    void deniesDialogsWithoutInvokingPresenter() {
        RecordingCallback callback = new RecordingCallback();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.DENY,
                request -> {
                    throw new AssertionError("Presenter must not be invoked");
                },
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                null,
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
                "Open",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));
        assertTrue(callback.cancelled);
    }

    @Test
    void cancelsFolderDialogsWithoutInvokingPresenter() {
        RecordingCallback callback = new RecordingCallback();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    throw new AssertionError("Presenter must not be invoked");
                },
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                null,
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN_FOLDER,
                "Folder",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));
        assertTrue(callback.cancelled);
    }

    @Test
    void routesArmedDirectoryPickersToFolderPresenter(@TempDir Path directory) {
        RecordingCallback callback = new RecordingCallback();
        AtomicReference<BrowserFileDialogPolicy.Request> policyRequest = new AtomicReference<>();
        BrowserOptions options = BrowserOptions.builder()
                .fileDialogPolicy(request -> {
                    policyRequest.set(request);
                    return BrowserFileDialogPolicy.Decision.ALLOW;
                })
                .build();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    assertEquals(BrowserFileDialogPresenter.Mode.OPEN_FOLDER, request.mode());
                    return CompletableFuture.completedFuture(List.of(directory));
                },
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                browser(options, true),
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
                "Folder",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));

        assertFalse(callback.cancelled);
        assertEquals(List.of(directory.toAbsolutePath().normalize().toString()), callback.paths);
        assertEquals(
                BrowserFileDialogPolicy.Source.FILE_SYSTEM_DIRECTORY_PICKER,
                policyRequest.get().source());
        assertEquals("app://assets/test/index.html", policyRequest.get().documentUrl());
    }

    @Test
    void rejectsDirectoryPickerWhenPolicyDenies(@TempDir Path directory) {
        boolean[] presenterCalled = {false};
        RecordingCallback callback = new RecordingCallback();
        BrowserOptions options = BrowserOptions.builder()
                .fileDialogPolicy(BrowserFileDialogPolicy.disabled())
                .build();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    presenterCalled[0] = true;
                    return CompletableFuture.completedFuture(List.of(directory));
                },
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                browser(options, true),
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
                "Folder",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));

        assertTrue(callback.cancelled);
        assertFalse(presenterCalled[0]);
    }

    @Test
    void rejectsDialogsWhenPolicyFails() {
        assertPolicyFailureCancels(request -> null);
        assertPolicyFailureCancels(request -> {
            throw new IllegalStateException("failed");
        });
    }

    @Test
    void rejectsFilesReturnedForDirectoryPicker(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("file.txt");
        Files.createFile(file);
        RecordingCallback callback = new RecordingCallback();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> CompletableFuture.completedFuture(List.of(file)),
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                browser(BrowserOptions.defaults(), true),
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
                "Folder",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));

        assertTrue(callback.cancelled);
    }

    @Test
    void cancelsWhenPresenterFailsAndHandlesSaveDialogs() {
        RecordingCallback callback = new RecordingCallback();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    throw new IllegalStateException("failed");
                },
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                browser(BrowserOptions.defaults(), false),
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
                "Open",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));
        assertTrue(callback.cancelled);

        RecordingCallback saveCallback = new RecordingCallback();
        GrapheneCefFileDialogHandler saveHandler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    assertEquals(BrowserFileDialogPresenter.Mode.SAVE_FILE, request.mode());
                    return CompletableFuture.completedFuture(List.of(Path.of("saved.txt")));
                },
                GrapheneTaskExecutor.direct());

        assertTrue(saveHandler.onFileDialog(
                browser(BrowserOptions.defaults(), false),
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

    private static CefBrowser browser(BrowserOptions options, boolean directoryIntent) {
        AtomicReference<Boolean> pendingDirectoryIntent = new AtomicReference<>(directoryIntent);
        return (CefBrowser) Proxy.newProxyInstance(
                CefBrowser.class.getClassLoader(),
                new Class<?>[] {CefBrowser.class, BrowserSession.class, GrapheneCefDirectoryPickerIntentSource.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "options" -> options;
                    case "currentUrl" -> "app://assets/test/index.html";
                    case "consumeDirectoryPickerIntent" -> pendingDirectoryIntent.getAndSet(false);
                    case "isClosed" -> false;
                    case "equals" -> proxy == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "TestBrowser";
                    default -> CefTestProxies.defaultValue(method.getReturnType());
                });
    }

    private static void assertPolicyFailureCancels(BrowserFileDialogPolicy policy) {
        RecordingCallback callback = new RecordingCallback();
        BrowserOptions options =
                BrowserOptions.builder().fileDialogPolicy(policy).build();
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(
                BrowserFileAccessPolicy.ALLOW,
                request -> {
                    throw new AssertionError("Presenter must not be invoked");
                },
                GrapheneTaskExecutor.direct());

        assertTrue(handler.onFileDialog(
                browser(options, false),
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
                "Open",
                "",
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                callback));
        assertTrue(callback.cancelled);
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
