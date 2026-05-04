package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class GrapheneCefBrowserShutdownTracker {
    private final Object lock = new Object();
    private final Set<Integer> openBrowserIdentifiers = new HashSet<>();
    private CompletableFuture<Void> allBrowsersClosedFuture = CompletableFuture.completedFuture(null);

    void onAfterCreated(CefBrowser browser) {
        int browserIdentifier = browser.getIdentifier();
        synchronized (lock) {
            if (openBrowserIdentifiers.isEmpty()) {
                allBrowsersClosedFuture = new CompletableFuture<>();
            }

            openBrowserIdentifiers.add(browserIdentifier);
        }
    }

    void onBeforeClose(CefBrowser browser) {
        int browserIdentifier = browser.getIdentifier();
        synchronized (lock) {
            openBrowserIdentifiers.remove(browserIdentifier);
            if (openBrowserIdentifiers.isEmpty()) {
                allBrowsersClosedFuture.complete(null);
            }
        }
    }

    CompletableFuture<Void> allBrowsersClosedFuture() {
        synchronized (lock) {
            if (openBrowserIdentifiers.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            return allBrowsersClosedFuture;
        }
    }

    int openBrowserCount() {
        synchronized (lock) {
            return openBrowserIdentifiers.size();
        }
    }
}
