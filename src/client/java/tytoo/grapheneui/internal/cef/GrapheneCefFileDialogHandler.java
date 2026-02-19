package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.internal.cef.alert.GrapheneFolderUploadDialogManager;

import java.util.Objects;
import java.util.Vector;

final class GrapheneCefFileDialogHandler implements CefDialogHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefFileDialogHandler.class);

    private final GrapheneFolderUploadDialogManager folderUploadDialogManager;

    GrapheneCefFileDialogHandler(GrapheneFolderUploadDialogManager folderUploadDialogManager) {
        this.folderUploadDialogManager = Objects.requireNonNull(folderUploadDialogManager, "folderUploadDialogManager");
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
            CefFileDialogCallback callback
    ) {
        if (mode != FileDialogMode.FILE_DIALOG_OPEN_FOLDER) {
            return false;
        }

        if (callback == null) {
            LOGGER.warn("Suppressed folder upload dialog without callback (title={})", title);
            return true;
        }

        folderUploadDialogManager.enqueueDialog(
                browser,
                title,
                defaultFilePath,
                acceptFilters,
                acceptExtensions,
                acceptDescriptions,
                callback
        );
        return true;
    }
}
