package io.github.trethore.graphene.api.runtime;

/** Read-only state and address information for the shared server hosting consumer asset mounts. */
@SuppressWarnings("unused")
public interface GrapheneHttpServer {
  boolean isRunning();

  String host();

  int port();

  String baseUrl();
}
