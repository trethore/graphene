package tytoo.grapheneui.internal.cef;

import org.cef.handler.CefDialogHandler;
import org.junit.jupiter.api.Test;
import tytoo.grapheneui.internal.cef.alert.GrapheneFolderUploadDialogDispatcher;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneCefFileDialogHandlerTest {
    @Test
    void suppressesFolderDialogWhenCallbackIsMissing() {
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(noOpDispatcher());

        boolean handled = handler.onFileDialog(
                null,
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN_FOLDER,
                "Select Folder",
                null,
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                null
        );

        assertTrue(handled);
    }

    @Test
    void ignoresNonFolderDialogs() {
        GrapheneCefFileDialogHandler handler = new GrapheneCefFileDialogHandler(noOpDispatcher());

        boolean handled = handler.onFileDialog(
                null,
                CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN,
                "Open File",
                null,
                new Vector<>(),
                new Vector<>(),
                new Vector<>(),
                null
        );

        assertFalse(handled);
    }

    private static GrapheneFolderUploadDialogDispatcher noOpDispatcher() {
        return (browser, title, defaultFilePath, callback) -> {
        };
    }
}
