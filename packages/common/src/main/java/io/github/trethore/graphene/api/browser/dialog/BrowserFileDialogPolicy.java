package io.github.trethore.graphene.api.browser.dialog;

import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.Objects;

/** Decides whether browser content may present a file dialog. */
@FunctionalInterface
public interface BrowserFileDialogPolicy {
    /**
     * Decides whether to allow a file-dialog request. This method is called synchronously on the
     * browser callback thread and must not block. Exceptions and {@code null} decisions deny the
     * request.
     */
    Decision decide(Request request);

    /** Allows file dialogs after the process-wide browser file-access policy has allowed them. */
    static BrowserFileDialogPolicy defaultPolicy() {
        return allowAll();
    }

    /** Allows every file dialog that passes the process-wide browser file-access policy. */
    static BrowserFileDialogPolicy allowAll() {
        return request -> Decision.ALLOW;
    }

    /** Denies all file dialogs for the browser session. */
    static BrowserFileDialogPolicy disabled() {
        return request -> Decision.DENY;
    }

    enum Decision {
        /** Allows Graphene to invoke the configured file-dialog presenter. */
        ALLOW,
        /** Cancels the browser request without invoking a presenter. */
        DENY
    }

    /** Identifies how Graphene classified the browser request. */
    enum Source {
        /** A file dialog that CEF did not identify as a File System Access directory picker. */
        BROWSER_DIALOG,
        /** A call to the File System Access API's {@code showDirectoryPicker()} method. */
        FILE_SYSTEM_DIRECTORY_PICKER
    }

    /** Immutable file-dialog information captured while the browser callback is valid. */
    record Request(
            BrowserSession session, String documentUrl, Source source, BrowserFileDialogPresenter.Request dialog) {
        public Request {
            Objects.requireNonNull(session, "session");
            Objects.requireNonNull(documentUrl, "documentUrl");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(dialog, "dialog");
        }
    }
}
