package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.misc.BoolRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.internal.cef.alert.GrapheneJsDialogManager;

import java.util.Objects;

final class GrapheneCefJsDialogHandler extends CefJSDialogHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefJsDialogHandler.class);

    private final GrapheneJsDialogManager dialogManager;

    GrapheneCefJsDialogHandler(GrapheneJsDialogManager dialogManager) {
        this.dialogManager = Objects.requireNonNull(dialogManager, "dialogManager");
    }

    @Override
    public boolean onJSDialog(
            CefBrowser browser,
            String originUrl,
            CefJSDialogHandler.JSDialogType dialogType,
            String messageText,
            String defaultPromptText,
            CefJSDialogCallback callback,
            BoolRef suppressMessage
    ) {
        if (callback == null) {
            if (suppressMessage != null) {
                suppressMessage.set(true);
            }

            LOGGER.warn(
                    "Suppressed JavaScript dialog without callback (type={}, origin={})",
                    dialogType,
                    originUrl
            );
            return false;
        }

        dialogManager.enqueueDialog(browser, originUrl, dialogType, messageText, defaultPromptText, callback);
        return true;
    }

    @Override
    public boolean onBeforeUnloadDialog(CefBrowser browser, String messageText, boolean isReload, CefJSDialogCallback callback) {
        if (browser == null) {
            LOGGER.warn("Cannot handle before-unload dialog without browser (reload={})", isReload);
            return false;
        }

        if (callback == null) {
            LOGGER.warn("Suppressed before-unload dialog without callback (reload={})", isReload);
            return true;
        }

        dialogManager.enqueueBeforeUnloadDialog(browser, messageText, isReload, callback);
        return true;
    }

    @Override
    public void onResetDialogState(CefBrowser browser) {
        if (browser == null) {
            return;
        }

        dialogManager.resetDialogState(browser);
    }

    @Override
    public void onDialogClosed(CefBrowser browser) {
        if (browser == null) {
            return;
        }

        dialogManager.onDialogClosed(browser);
    }
}
