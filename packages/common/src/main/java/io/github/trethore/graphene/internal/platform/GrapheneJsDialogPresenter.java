package io.github.trethore.graphene.internal.platform;

import java.util.concurrent.CompletionStage;

public interface GrapheneJsDialogPresenter {
  CompletionStage<Result> show(
      DialogType type, String originUrl, String message, String defaultPrompt);

  enum DialogType {
    ALERT,
    CONFIRM,
    PROMPT,
    BEFORE_UNLOAD
  }

  record Result(boolean accepted, String promptText) {}
}
