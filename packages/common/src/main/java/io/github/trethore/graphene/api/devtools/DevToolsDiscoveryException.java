package io.github.trethore.graphene.api.devtools;

/** Thrown when the remote DevTools discovery endpoint cannot be queried or parsed. */
public final class DevToolsDiscoveryException extends RuntimeException {
  public DevToolsDiscoveryException(String message) {
    super(message);
  }

  public DevToolsDiscoveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
