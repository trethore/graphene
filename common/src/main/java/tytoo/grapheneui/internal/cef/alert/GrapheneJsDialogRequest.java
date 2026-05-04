package tytoo.grapheneui.internal.cef.alert;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;

import java.util.concurrent.atomic.AtomicBoolean;

final class GrapheneJsDialogRequest {
    private static final String EMPTY_VALUE = "";
    private static final String DEFAULT_BEFORE_UNLOAD_MESSAGE = "Do you want to leave this page?";
    private static final String DEFAULT_BEFORE_RELOAD_MESSAGE = "Do you want to reload this page?";
    private final CefBrowser browser;
    private final String originUrl;
    private final DialogKind dialogKind;
    private final String messageText;
    private final String defaultPromptText;
    private final CefJSDialogCallback callback;
    private final AtomicBoolean resolved = new AtomicBoolean(false);

    private GrapheneJsDialogRequest(
            CefBrowser browser,
            String originUrl,
            DialogKind dialogKind,
            String messageText,
            String defaultPromptText,
            CefJSDialogCallback callback
    ) {
        this.browser = browser;
        this.originUrl = normalizeString(originUrl);
        this.dialogKind = dialogKind;
        this.messageText = normalizeString(messageText);
        this.defaultPromptText = normalizeString(defaultPromptText);
        this.callback = callback;
    }

    static GrapheneJsDialogRequest jsDialog(
            CefBrowser browser,
            String originUrl,
            CefJSDialogHandler.JSDialogType dialogType,
            String messageText,
            String defaultPromptText,
            CefJSDialogCallback callback
    ) {
        DialogKind dialogKind = switch (dialogType) {
            case JSDIALOGTYPE_ALERT -> DialogKind.ALERT;
            case JSDIALOGTYPE_CONFIRM -> DialogKind.CONFIRM;
            case JSDIALOGTYPE_PROMPT -> DialogKind.PROMPT;
        };
        return new GrapheneJsDialogRequest(browser, originUrl, dialogKind, messageText, defaultPromptText, callback);
    }

    static GrapheneJsDialogRequest beforeUnloadDialog(
            CefBrowser browser,
            String messageText,
            boolean isReload,
            CefJSDialogCallback callback
    ) {
        String normalizedMessageText = normalizeString(messageText);
        if (normalizedMessageText.isBlank()) {
            normalizedMessageText = isReload ? DEFAULT_BEFORE_RELOAD_MESSAGE : DEFAULT_BEFORE_UNLOAD_MESSAGE;
        }

        return new GrapheneJsDialogRequest(browser, EMPTY_VALUE, DialogKind.BEFORE_UNLOAD, normalizedMessageText, EMPTY_VALUE, callback);
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

    DialogKind dialogKind() {
        return dialogKind;
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
        if (dialogKind == DialogKind.ALERT) {
            return true;
        }

        return accepted;
    }

    String normalizeValue(boolean accepted, String value) {
        if (dialogKind != DialogKind.PROMPT || !accepted) {
            return EMPTY_VALUE;
        }

        return normalizeString(value);
    }

    String logType() {
        return switch (dialogKind) {
            case ALERT -> "alert";
            case CONFIRM -> "confirm";
            case PROMPT -> "prompt";
            case BEFORE_UNLOAD -> "before-unload";
        };
    }

    enum DialogKind {
        ALERT,
        CONFIRM,
        PROMPT,
        BEFORE_UNLOAD
    }
}
