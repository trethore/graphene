package tytoo.grapheneui.internal.cef.alert;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class GrapheneFolderUploadDialogRequest {
    private final CefBrowser browser;
    private final String title;
    private final String defaultFilePath;
    private final List<String> acceptFilters;
    private final List<String> acceptExtensions;
    private final List<String> acceptDescriptions;
    private final CefFileDialogCallback callback;
    private final AtomicBoolean resolved = new AtomicBoolean(false);

    GrapheneFolderUploadDialogRequest(
            CefBrowser browser,
            String title,
            String defaultFilePath,
            List<String> acceptFilters,
            List<String> acceptExtensions,
            List<String> acceptDescriptions,
            CefFileDialogCallback callback
    ) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.title = title == null ? "" : title;
        this.defaultFilePath = defaultFilePath == null ? "" : defaultFilePath;
        this.acceptFilters = copyList(acceptFilters);
        this.acceptExtensions = copyList(acceptExtensions);
        this.acceptDescriptions = copyList(acceptDescriptions);
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    private static List<String> copyList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(source);
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

    List<String> acceptFilters() {
        return new ArrayList<>(acceptFilters);
    }

    List<String> acceptExtensions() {
        return new ArrayList<>(acceptExtensions);
    }

    List<String> acceptDescriptions() {
        return new ArrayList<>(acceptDescriptions);
    }

    CefFileDialogCallback callback() {
        return callback;
    }

    boolean tryResolve() {
        return resolved.compareAndSet(false, true);
    }
}
