package tytoo.grapheneui.cef.alert;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;

import java.util.concurrent.atomic.AtomicBoolean;

final class GrapheneJsDialogRequest {
    private static final String EMPTY_VALUE = "";

    private final CefBrowser browser;
    private final String originUrl;
    private final CefJSDialogHandler.JSDialogType dialogType;
    private final String messageText;
    private final String defaultPromptText;
    private final CefJSDialogCallback callback;
    private final AtomicBoolean resolved = new AtomicBoolean(false);

    GrapheneJsDialogRequest(
            CefBrowser browser,
            String originUrl,
            CefJSDialogHandler.JSDialogType dialogType,
            String messageText,
            String defaultPromptText,
            CefJSDialogCallback callback
    ) {
        this.browser = browser;
        this.originUrl = normalizeString(originUrl);
        this.dialogType = dialogType;
        this.messageText = normalizeString(messageText);
        this.defaultPromptText = normalizeString(defaultPromptText);
        this.callback = callback;
    }

    private static String normalizeString(String value) {
        return value == null ? EMPTY_VALUE : value;
    }

    CefBrowser browser() {
        return browser;
    }

    String originUrl() {
        return originUrl;
    }

    CefJSDialogHandler.JSDialogType dialogType() {
        return dialogType;
    }

    String messageText() {
        return messageText;
    }

    String defaultPromptText() {
        return defaultPromptText;
    }

    CefJSDialogCallback callback() {
        return callback;
    }

    boolean tryResolve() {
        return resolved.compareAndSet(false, true);
    }

    boolean normalizeAccepted(boolean accepted) {
        if (dialogType == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT) {
            return true;
        }

        return accepted;
    }

    String normalizeValue(boolean accepted, String value) {
        if (dialogType != CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_PROMPT || !accepted) {
            return EMPTY_VALUE;
        }

        return normalizeString(value);
    }
}
