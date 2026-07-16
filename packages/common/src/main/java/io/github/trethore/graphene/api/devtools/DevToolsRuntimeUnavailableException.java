package io.github.trethore.graphene.api.devtools;

import io.github.trethore.graphene.api.runtime.GrapheneRuntimeState;
import java.util.Objects;

/** Thrown when DevTools discovery is requested while the Graphene runtime is not running. */
public final class DevToolsRuntimeUnavailableException extends IllegalStateException {
  private final GrapheneRuntimeState runtimeState;

  public DevToolsRuntimeUnavailableException(GrapheneRuntimeState runtimeState) {
    super(message(runtimeState));
    this.runtimeState = runtimeState;
  }

  public GrapheneRuntimeState runtimeState() {
    return runtimeState;
  }

  private static String message(GrapheneRuntimeState runtimeState) {
    GrapheneRuntimeState validatedState = Objects.requireNonNull(runtimeState, "runtimeState");
    return "Remote DevTools discovery requires a running Graphene runtime; current state is "
        + validatedState;
  }
}
