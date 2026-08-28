package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPolicy;
import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.config.BrowserFileAccessPolicy;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneCefFileDialogHandler implements CefDialogHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefFileDialogHandler.class);

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
                        && browser instanceof GrapheneCefDirectoryPickerIntentSource intentSource
                        && intentSource.consumeDirectoryPickerIntent();
        if (mode == FileDialogMode.FILE_DIALOG_OPEN_FOLDER && !directoryIntent) {
            // CEF displays an unhandled Chromium confirmation dialog after upload-folder selection,
            // which is unsafe with off-screen rendering.
            callback.Cancel();
            return true;
        }
        if (!(browser instanceof BrowserSession session)) {
            callback.Cancel();
            return true;
        }
        BrowserFileDialogPresenter.Request request = new BrowserFileDialogPresenter.Request(
                directoryIntent ? BrowserFileDialogPresenter.Mode.OPEN_FOLDER : mode(mode),
                Objects.requireNonNullElse(title, ""),
                Objects.requireNonNullElse(defaultFilePath, ""),
                filters(acceptFilters, acceptExtensions, acceptDescriptions));
        BrowserFileDialogPolicy.Source source = directoryIntent
                ? BrowserFileDialogPolicy.Source.FILE_SYSTEM_DIRECTORY_PICKER
                : BrowserFileDialogPolicy.Source.BROWSER_DIALOG;
        if (!allows(session, documentUrl(browser, session), source, request)) {
            callback.Cancel();
            return true;
        }
        BrowserFileDialogPresenter presenter =
                session.options().fileDialogPresenter().orElse(defaultPresenter);
        mainThreadExecutor
                .supplyStage(() -> presenter.show(request))
                .whenComplete((paths, failure) ->
                        mainThreadExecutor.execute(() -> complete(callback, request.mode(), paths, failure)));
        return true;
    }

    private static boolean allows(
            BrowserSession session,
            String documentUrl,
            BrowserFileDialogPolicy.Source source,
            BrowserFileDialogPresenter.Request dialogRequest) {
        BrowserFileDialogPolicy.Request policyRequest =
                new BrowserFileDialogPolicy.Request(session, documentUrl, source, dialogRequest);
        try {
            BrowserFileDialogPolicy.Decision decision =
                    session.options().fileDialogPolicy().decide(policyRequest);
            if (decision != null) {
                return decision == BrowserFileDialogPolicy.Decision.ALLOW;
            }
            LOGGER.warn("Browser file-dialog policy returned null for {}", policyRequest.documentUrl());
        } catch (RuntimeException exception) {
            LOGGER.warn("Browser file-dialog policy failed for {}", policyRequest.documentUrl(), exception);
        }
        return false;
    }

    private static String documentUrl(CefBrowser browser, BrowserSession session) {
        try {
            return Objects.requireNonNullElse(browser.getURL(), session.currentUrl());
        } catch (RuntimeException exception) {
            return session.currentUrl();
        }
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
            List<String> values, List<String> extensions, List<String> descriptions) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<BrowserFileDialogPresenter.Filter> filters = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            filters.add(new BrowserFileDialogPresenter.Filter(
                    valueAt(values, index), valueAt(extensions, index), valueAt(descriptions, index)));
        }
        return filters;
    }

    private static String valueAt(List<String> values, int index) {
        if (values == null || index >= values.size()) {
            return "";
        }
        return Objects.requireNonNullElse(values.get(index), "");
    }

    @SuppressWarnings("java:S1149")
    private static void complete(
            CefFileDialogCallback callback, BrowserFileDialogPresenter.Mode mode, List<Path> paths, Throwable failure) {
        if (failure != null || paths == null || paths.isEmpty()) {
            callback.Cancel();
            return;
        }
        if (mode == BrowserFileDialogPresenter.Mode.OPEN_FOLDER
                && (paths.size() != 1 || !isDirectory(paths.getFirst()))) {
            callback.Cancel();
            return;
        }
        Vector<String> selectedPaths = new Vector<>();
        try {
            for (Path path : paths) {
                selectedPaths.add(path.toAbsolutePath().normalize().toString());
            }
        } catch (RuntimeException exception) {
            callback.Cancel();
            return;
        }
        callback.Continue(selectedPaths);
    }

    private static boolean isDirectory(Path path) {
        try {
            return path != null && Files.isDirectory(path);
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
