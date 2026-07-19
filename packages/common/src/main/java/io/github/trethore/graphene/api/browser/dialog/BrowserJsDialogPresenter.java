package io.github.trethore.graphene.api.browser.dialog;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Presents JavaScript dialogs through a consumer-provided user interface. */
@FunctionalInterface
public interface BrowserJsDialogPresenter {
  /** Presents the request asynchronously. Failed or {@code null} completion rejects the dialog. */
  CompletionStage<Result> show(Request request);

  /** Kind of JavaScript dialog requested by browser content. */
  enum Type {
    ALERT,
    CONFIRM,
    PROMPT,
    BEFORE_UNLOAD
  }

  /** Immutable details of a JavaScript dialog request. */
  record Request(
      Type type, String originUrl, String message, String defaultPrompt, boolean reload) {
    public Request {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(originUrl, "originUrl");
      Objects.requireNonNull(message, "message");
      Objects.requireNonNull(defaultPrompt, "defaultPrompt");
    }
  }

  /** The consumer-selected outcome of a JavaScript dialog. */
  record Result(boolean accepted, String promptText) {
    public Result {
      Objects.requireNonNull(promptText, "promptText");
    }

    public static Result accept(String promptText) {
      return new Result(true, promptText);
    }

    public static Result accept() {
      return accept("");
    }

    public static Result cancel() {
      return new Result(false, "");
    }
  }
}
