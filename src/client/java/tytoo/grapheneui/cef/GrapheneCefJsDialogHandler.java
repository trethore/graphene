package tytoo.grapheneui.cef;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.misc.BoolRef;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.cef.alert.GrapheneJsDialogManager;

import java.util.Objects;

final class GrapheneCefJsDialogHandler extends CefJSDialogHandlerAdapter {
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

            GrapheneCore.LOGGER.warn(
                    "Suppressed JavaScript dialog without callback (type={}, origin={})",
                    dialogType,
                    originUrl
            );
            return false;
        }

        dialogManager.enqueueDialog(browser, originUrl, dialogType, messageText, defaultPromptText, callback);
        return true;
    }
}
