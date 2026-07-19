package io.github.trethore.graphene.api.devtools;

/** Thrown when remote DevTools discovery is requested while remote debugging is disabled. */
public final class DevToolsDisabledException extends IllegalStateException {
  public DevToolsDisabledException() {
    super("Remote DevTools debugging is disabled");
  }
}
