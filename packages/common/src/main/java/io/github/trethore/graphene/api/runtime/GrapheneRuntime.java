package io.github.trethore.graphene.api.runtime;

import java.util.OptionalInt;
import java.util.concurrent.CompletionStage;

@SuppressWarnings("unused")
public interface GrapheneRuntime {
  GrapheneRuntimeState state();

  boolean isInitialized();

  CompletionStage<Void> initialization();

  OptionalInt remoteDebuggingPort();

  GrapheneHttpServer httpServer();
}
