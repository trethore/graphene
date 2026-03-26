package tytoo.grapheneui.internal.cef.alert;

import net.minecraft.client.gui.screens.Screen;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.mc.McClient;

import java.util.ArrayDeque;
import java.util.Deque;

public final class GrapheneJsDialogManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneJsDialogManager.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneJsDialogManager.class);
    private static final String EMPTY_VALUE = "";

    private final Object lock = new Object();
    private final Deque<GrapheneJsDialogRequest> pendingDialogs = new ArrayDeque<>();
    private boolean dialogVisible;

    public void enqueueDialog(
            CefBrowser browser,
            String originUrl,
            CefJSDialogHandler.JSDialogType dialogType,
            String messageText,
            String defaultPromptText,
            CefJSDialogCallback callback
    ) {
        GrapheneJsDialogRequest request = GrapheneJsDialogRequest.jsDialog(
                browser,
                originUrl,
                dialogType,
                messageText,
                defaultPromptText,
                callback
        );

        enqueueRequest(request, originUrl);
    }

    public void enqueueBeforeUnloadDialog(
            CefBrowser browser,
            String messageText,
            boolean isReload,
            CefJSDialogCallback callback
    ) {
        GrapheneJsDialogRequest request = GrapheneJsDialogRequest.beforeUnloadDialog(browser, messageText, isReload, callback);
        enqueueRequest(request, isReload ? "reload" : "unload");
    }

    public void resetDialogState(CefBrowser browser) {
        cancelDialogs(browser, "reset");
    }

    public void onDialogClosed(CefBrowser browser) {
        cancelDialogs(browser, "closed");
    }

    private void enqueueRequest(GrapheneJsDialogRequest request, String context) {
        boolean shouldOpen;
        synchronized (lock) {
            pendingDialogs.addLast(request);
            shouldOpen = !dialogVisible;
            if (shouldOpen) {
                dialogVisible = true;
            }

            DEBUG_LOGGER.debug(
                    "Queued JS dialog type={} context={} pending={} shouldOpen={}",
                    request.logType(),
                    context,
                    pendingDialogs.size(),
                    shouldOpen
            );
        }

        if (!shouldOpen) {
            return;
        }

        McClient.execute(this::displayNextDialogOnClientThread);
    }

    private void displayNextDialogOnClientThread() {
        GrapheneJsDialogRequest request;
        synchronized (lock) {
            request = pendingDialogs.peekFirst();
            if (request == null) {
                dialogVisible = false;
                return;
            }
        }

        Screen currentScreen = McClient.currentScreen();
        Screen returnScreen = currentScreen instanceof GrapheneJsDialogScreen dialogScreen
                ? dialogScreen.returnScreen()
                : currentScreen;

        GrapheneJsDialogScreen screen = new GrapheneJsDialogScreen(
                request,
                returnScreen,
                this::resolveDialog
        );
        McClient.setScreen(screen);
    }

    private void resolveDialog(GrapheneJsDialogScreen screen, boolean accepted, String value) {
        GrapheneJsDialogRequest request = screen.request();
        if (!request.tryResolve()) {
            return;
        }

        boolean hasMore;
        synchronized (lock) {
            GrapheneJsDialogRequest current = pendingDialogs.peekFirst();
            if (current == request) {
                pendingDialogs.removeFirst();
            } else {
                pendingDialogs.remove(request);
            }

            hasMore = !pendingDialogs.isEmpty();
            dialogVisible = hasMore;

            DEBUG_LOGGER.debug(
                    "Resolved JS dialog type={} accepted={} pending={} hasMore={}",
                    request.logType(),
                    request.normalizeAccepted(accepted),
                    pendingDialogs.size(),
                    hasMore
            );
        }

        if (McClient.currentScreen() == screen) {
            McClient.setScreen(screen.returnScreen());
        }

        continueRequest(request, accepted, value);

        if (hasMore) {
            McClient.execute(this::displayNextDialogOnClientThread);
        }
    }

    private void cancelDialogs(CefBrowser browser, String reason) {
        boolean activeDialogRemoved;
        boolean hasMore;
        Deque<GrapheneJsDialogRequest> requestsToCancel = new ArrayDeque<>();
        synchronized (lock) {
            GrapheneJsDialogRequest activeDialog = pendingDialogs.peekFirst();
            activeDialogRemoved = activeDialog != null && matchesBrowser(activeDialog, browser);

            pendingDialogs.removeIf(request -> {
                if (!matchesBrowser(request, browser)) {
                    return false;
                }

                requestsToCancel.addLast(request);
                return true;
            });

            hasMore = !pendingDialogs.isEmpty();
            if (activeDialogRemoved) {
                dialogVisible = hasMore;
            }
        }

        if (requestsToCancel.isEmpty()) {
            return;
        }

        for (GrapheneJsDialogRequest request : requestsToCancel) {
            if (!request.tryResolve()) {
                continue;
            }

            continueRequest(request, false, EMPTY_VALUE);
            DEBUG_LOGGER.debug("Canceled JS dialog type={} reason={}", request.logType(), reason);
        }

        if (!activeDialogRemoved) {
            return;
        }

        McClient.execute(() -> {
            Screen currentScreen = McClient.currentScreen();
            if (currentScreen instanceof GrapheneJsDialogScreen dialogScreen && matchesBrowser(dialogScreen.request(), browser)) {
                McClient.setScreen(dialogScreen.returnScreen());
            }

            if (hasMore) {
                displayNextDialogOnClientThread();
            }
        });
    }

    private void continueRequest(GrapheneJsDialogRequest request, boolean accepted, String value) {
        boolean normalizedAccepted = request.normalizeAccepted(accepted);
        String normalizedValue = request.normalizeValue(normalizedAccepted, value);

        try {
            request.callback().Continue(normalizedAccepted, normalizedValue);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to continue JavaScript dialog callback for {} ({})",
                    request.originUrl(),
                    request.browser(),
                    exception
            );
        }
    }

    private boolean matchesBrowser(GrapheneJsDialogRequest request, CefBrowser browser) {
        return request.browser() == browser;
    }
}
