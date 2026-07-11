package io.github.trethore.graphene.api.runtime;

import java.util.OptionalInt;
import java.util.concurrent.CompletionStage;

@SuppressWarnings("unused")
public interface GrapheneRuntime {
  GrapheneRuntimeState state();

  CompletionStage<Void> initializeAsync();

  void initialize();

  CompletionStage<Void> shutdownAsync();

  boolean isInitialized();

  OptionalInt remoteDebuggingPort();

  GrapheneHttpServer httpServer();
}
