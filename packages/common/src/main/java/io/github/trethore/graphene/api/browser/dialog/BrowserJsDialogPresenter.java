package io.github.trethore.graphene.api.browser.dialog;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface BrowserJsDialogPresenter {
  CompletionStage<Result> show(Request request);

  enum Type {
    ALERT,
    CONFIRM,
    PROMPT,
    BEFORE_UNLOAD
  }

  record Request(
      Type type, String originUrl, String message, String defaultPrompt, boolean reload) {
    public Request {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(originUrl, "originUrl");
      Objects.requireNonNull(message, "message");
      Objects.requireNonNull(defaultPrompt, "defaultPrompt");
    }
  }

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
