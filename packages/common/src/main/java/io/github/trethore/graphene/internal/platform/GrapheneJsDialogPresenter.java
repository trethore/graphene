package io.github.trethore.graphene.internal.platform;

import java.util.concurrent.CompletionStage;

public interface GrapheneJsDialogPresenter {
  CompletionStage<Result> show(String originUrl, String message, String defaultPrompt);

  record Result(boolean accepted, String promptText) {}
}
