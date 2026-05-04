package tytoo.grapheneui.internal.cef.alert;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;

public interface GrapheneFolderUploadDialogDispatcher {
    void enqueueDialog(
            CefBrowser browser,
            String title,
            String defaultFilePath,
            CefFileDialogCallback callback
    );
}
