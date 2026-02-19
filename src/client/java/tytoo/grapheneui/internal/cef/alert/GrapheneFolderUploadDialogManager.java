package tytoo.grapheneui.internal.cef.alert;

import net.minecraft.client.gui.screens.Screen;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.mc.McClient;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GrapheneFolderUploadDialogManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneFolderUploadDialogManager.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneFolderUploadDialogManager.class);
    private static final ExecutorService DIALOG_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "graphene-folder-upload-dialog");
        thread.setDaemon(true);
        return thread;
    });

    private final Object lock = new Object();
    private final Deque<GrapheneFolderUploadDialogRequest> pendingDialogs = new ArrayDeque<>();
    private boolean dialogVisible;

    private static Path selectFolder(GrapheneFolderUploadDialogRequest request) {
        List<String> acceptFilters = request.acceptFilters();
        List<String> acceptExtensions = request.acceptExtensions();
        List<String> acceptDescriptions = request.acceptDescriptions();

        DEBUG_LOGGER.debug(
                "Opening folder chooser title={} filters={} extensions={} descriptions={}",
                request.title(),
                acceptFilters.size(),
                acceptExtensions.size(),
                acceptDescriptions.size()
        );

        Path selectedFolder = openNativeDirectoryChooser(request.title(), request.defaultFilePath());
        if (selectedFolder == null) {
            return null;
        }

        if (!Files.isDirectory(selectedFolder)) {
            LOGGER.warn("Selected path is not a directory: {}", selectedFolder);
            return null;
        }

        return selectedFolder;
    }

    private static Vector<String> collectUploadFilePaths(Path folder) {
        try (Stream<Path> pathStream = Files.walk(folder)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(path -> path.toAbsolutePath().normalize().toString())
                    .collect(Collectors.toCollection(Vector::new));
        } catch (Exception exception) {
            LOGGER.warn("Failed to enumerate files for folder upload {}", folder, exception);
            return new Vector<>();
        }
    }

    private static Path openNativeDirectoryChooser(String title, String defaultFilePath) {
        String normalizedTitle = (title == null || title.isBlank()) ? "Select Folder" : title;
        String normalizedDefaultPath = normalizeDefaultDirectory(defaultFilePath);
        try {
            String selectedPath = TinyFileDialogs.tinyfd_selectFolderDialog(normalizedTitle, normalizedDefaultPath);
            if (selectedPath == null || selectedPath.isBlank()) {
                return null;
            }

            return Path.of(selectedPath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            LOGGER.warn("Folder chooser returned invalid path: {}", exception.getInput());
            return null;
        } catch (RuntimeException | UnsatisfiedLinkError exception) {
            LOGGER.error("Failed to open native folder chooser", exception);
            return null;
        }
    }

    private static String normalizeDefaultDirectory(String defaultFilePath) {
        if (defaultFilePath == null || defaultFilePath.isBlank()) {
            return null;
        }

        try {
            Path defaultPath = Path.of(defaultFilePath).toAbsolutePath().normalize();
            if (Files.isDirectory(defaultPath)) {
                return defaultPath.toString();
            }

            Path parent = defaultPath.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                return parent.toString();
            }
        } catch (InvalidPathException ignored) {
            // Ignore malformed defaults provided by web content.
            return null;
        }

        return null;
    }

    public void enqueueDialog(
            CefBrowser browser,
            String title,
            String defaultFilePath,
            Vector<String> acceptFilters,
            Vector<String> acceptExtensions,
            Vector<String> acceptDescriptions,
            CefFileDialogCallback callback
    ) {
        GrapheneFolderUploadDialogRequest request = new GrapheneFolderUploadDialogRequest(
                browser,
                title,
                defaultFilePath,
                acceptFilters,
                acceptExtensions,
                acceptDescriptions,
                callback
        );

        boolean shouldOpen;
        synchronized (lock) {
            pendingDialogs.addLast(request);
            shouldOpen = !dialogVisible;
            if (shouldOpen) {
                dialogVisible = true;
            }

            DEBUG_LOGGER.debug(
                    "Queued folder upload dialog title={} pending={} shouldOpen={}",
                    title,
                    pendingDialogs.size(),
                    shouldOpen
            );
        }

        if (!shouldOpen) {
            return;
        }

        displayNextDialog();
    }

    private void displayNextDialog() {
        GrapheneFolderUploadDialogRequest request;
        synchronized (lock) {
            request = pendingDialogs.peekFirst();
            if (request == null) {
                dialogVisible = false;
                return;
            }
        }

        CompletableFuture
                .supplyAsync(() -> selectFolder(request), DIALOG_EXECUTOR)
                .whenComplete((selectedFolder, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Failed to process folder upload chooser", throwable);
                        McClient.execute(() -> resolveCancel(request, null, null));
                        return;
                    }

                    if (selectedFolder == null) {
                        McClient.execute(() -> resolveCancel(request, null, null));
                        return;
                    }

                    Vector<String> selectedPaths = collectUploadFilePaths(selectedFolder);
                    long fileCount = selectedPaths.size();
                    McClient.execute(() -> showFolderUploadDialog(request, selectedFolder, selectedPaths, fileCount));
                });
    }

    private void showFolderUploadDialog(
            GrapheneFolderUploadDialogRequest request,
            Path selectedFolder,
            Vector<String> selectedPaths,
            long fileCount
    ) {
        Screen currentScreen = McClient.currentScreen();
        Screen returnScreen = currentScreen instanceof GrapheneFolderUploadDialogScreen dialogScreen
                ? dialogScreen.returnScreen()
                : currentScreen;

        GrapheneFolderUploadDialogScreen screen = new GrapheneFolderUploadDialogScreen(
                request,
                returnScreen,
                this::resolveFromScreen,
                selectedPaths,
                selectedFolder,
                fileCount
        );
        McClient.setScreen(screen);
    }

    private void resolveFromScreen(GrapheneFolderUploadDialogScreen screen, boolean accepted) {
        GrapheneFolderUploadDialogRequest request = screen.request();
        Screen returnScreen = screen.returnScreen();
        if (accepted) {
            resolveContinue(request, screen.selectedPaths(), screen, returnScreen);
            return;
        }

        resolveCancel(request, screen, returnScreen);
    }

    private void resolveContinue(
            GrapheneFolderUploadDialogRequest request,
            Vector<String> selectedPaths,
            Screen dialogScreen,
            Screen returnScreen
    ) {
        if (!request.tryResolve()) {
            return;
        }

        try {
            request.callback().Continue(selectedPaths);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to continue folder upload callback for {}", request.browser(), exception);
        }

        finalizeDialog(request, dialogScreen, returnScreen);
    }

    private void resolveCancel(GrapheneFolderUploadDialogRequest request, Screen dialogScreen, Screen returnScreen) {
        if (!request.tryResolve()) {
            return;
        }

        try {
            request.callback().Cancel();
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to cancel folder upload callback for {}", request.browser(), exception);
        }

        finalizeDialog(request, dialogScreen, returnScreen);
    }

    private void finalizeDialog(GrapheneFolderUploadDialogRequest request, Screen dialogScreen, Screen returnScreen) {
        boolean hasMore;
        synchronized (lock) {
            GrapheneFolderUploadDialogRequest current = pendingDialogs.peekFirst();
            if (current == request) {
                pendingDialogs.removeFirst();
            } else {
                pendingDialogs.remove(request);
            }

            hasMore = !pendingDialogs.isEmpty();
            dialogVisible = hasMore;

            DEBUG_LOGGER.debug("Resolved folder upload dialog pending={} hasMore={}", pendingDialogs.size(), hasMore);
        }

        if (dialogScreen != null && McClient.currentScreen() == dialogScreen) {
            McClient.setScreen(returnScreen);
        }

        if (hasMore) {
            displayNextDialog();
        }
    }
}
