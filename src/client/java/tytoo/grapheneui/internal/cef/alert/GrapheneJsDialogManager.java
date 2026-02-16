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
        GrapheneJsDialogRequest request = new GrapheneJsDialogRequest(
                browser,
                originUrl,
                dialogType,
                messageText,
                defaultPromptText,
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
                    "Queued JS dialog type={} origin={} pending={} shouldOpen={}",
                    dialogType,
                    originUrl,
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
                    request.dialogType(),
                    normalizedAccepted,
                    pendingDialogs.size(),
                    hasMore
            );
        }

        if (McClient.currentScreen() == screen) {
            McClient.setScreen(screen.returnScreen());
        }

        if (hasMore) {
            McClient.execute(this::displayNextDialogOnClientThread);
        }
    }
}
