package io.github.trethore.graphene.api.browser;

import io.github.trethore.graphene.api.runtime.GrapheneRuntimeState;
import java.util.Objects;

/** Thrown when a browser session is requested while the Graphene runtime is not running. */
public final class BrowserRuntimeUnavailableException extends IllegalStateException {
  private final GrapheneRuntimeState runtimeState;

  public BrowserRuntimeUnavailableException(GrapheneRuntimeState runtimeState, Throwable cause) {
    super(message(runtimeState), cause);
    this.runtimeState = runtimeState;
  }

  /** Returns the runtime state observed when browser creation was rejected. */
  public GrapheneRuntimeState runtimeState() {
    return runtimeState;
  }

  private static String message(GrapheneRuntimeState runtimeState) {
    GrapheneRuntimeState validatedState = Objects.requireNonNull(runtimeState, "runtimeState");
    return switch (validatedState) {
      case NEW -> "Cannot create a browser session before Graphene startup has begun";
      case STARTING ->
          "Cannot create a browser session while Graphene is starting; await runtime initialization";
      case FAILED -> "Cannot create a browser session because Graphene initialization failed";
      case STOPPING -> "Cannot create a browser session while Graphene is stopping";
      case STOPPED -> "Cannot create a browser session after Graphene has stopped";
      case RUNNING -> "Cannot create a browser session because Graphene is unavailable";
    };
  }
}
