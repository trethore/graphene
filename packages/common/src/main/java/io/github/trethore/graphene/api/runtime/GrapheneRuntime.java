package io.github.trethore.graphene.api.runtime;

import java.util.OptionalInt;
import java.util.concurrent.CompletionStage;

@SuppressWarnings("unused")
public interface GrapheneRuntime {
  GrapheneRuntimeState state();

  boolean isInitialized();

  /**
   * Returns the process-wide initialization stage.
   *
   * <p>The stage completes successfully when the runtime first reaches {@link
   * GrapheneRuntimeState#RUNNING}. It completes exceptionally if initialization fails or the
   * runtime stops before becoming ready. Successful completion remains observable after a later
   * shutdown and does not reserve availability against concurrent shutdown.
   *
   * <p>Graphene does not guarantee an executor for dependent stage actions. Callers that require a
   * platform thread must select an executor through an asynchronous completion-stage method.
   */
  CompletionStage<Void> initialization();

  OptionalInt remoteDebuggingPort();

  GrapheneHttpServer httpServer();
}
