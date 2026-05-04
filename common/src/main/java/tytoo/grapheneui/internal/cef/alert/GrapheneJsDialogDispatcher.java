package tytoo.grapheneui.internal.cef.alert;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;

public interface GrapheneJsDialogDispatcher {
    void enqueueDialog(
            CefBrowser browser,
            String originUrl,
            CefJSDialogHandler.JSDialogType dialogType,
            String messageText,
            String defaultPromptText,
            CefJSDialogCallback callback
    );

    void enqueueBeforeUnloadDialog(
            CefBrowser browser,
            String messageText,
            boolean isReload,
            CefJSDialogCallback callback
    );

    void resetDialogState(CefBrowser browser);

    void onDialogClosed(CefBrowser browser);
}
