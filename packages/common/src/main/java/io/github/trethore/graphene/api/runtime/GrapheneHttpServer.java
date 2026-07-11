package io.github.trethore.graphene.api.runtime;

@SuppressWarnings("unused")
public interface GrapheneHttpServer {
  boolean isRunning();

  String host();

  int port();

  String baseUrl();
}
