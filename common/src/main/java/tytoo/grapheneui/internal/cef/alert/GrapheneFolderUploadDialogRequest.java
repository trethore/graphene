package tytoo.grapheneui.internal.cef.alert;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class GrapheneFolderUploadDialogRequest {
    private final CefBrowser browser;
    private final String title;
    private final String defaultFilePath;
    private final CefFileDialogCallback callback;
    private final AtomicBoolean resolved = new AtomicBoolean(false);

    GrapheneFolderUploadDialogRequest(
            CefBrowser browser,
            String title,
            String defaultFilePath,
            CefFileDialogCallback callback
    ) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.title = title == null ? "" : title;
        this.defaultFilePath = defaultFilePath == null ? "" : defaultFilePath;
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    CefBrowser browser() {
        return browser;
    }

    String title() {
        return title;
    }

    String defaultFilePath() {
        return defaultFilePath;
    }

    CefFileDialogCallback callback() {
        return callback;
    }

    boolean tryResolve() {
        return resolved.compareAndSet(false, true);
    }
}
