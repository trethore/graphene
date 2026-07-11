package io.github.trethore.graphene.api;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class GrapheneBackendRegistry {
  private static final AtomicReference<GrapheneBackend> BACKEND = new AtomicReference<>();

  private GrapheneBackendRegistry() {}

  public static synchronized void install(GrapheneBackend installedBackend) {
    Objects.requireNonNull(installedBackend, "installedBackend");
    GrapheneBackend existingBackend = BACKEND.get();
    if (existingBackend != null && existingBackend != installedBackend) {
      throw new IllegalStateException("A Graphene backend is already installed");
    }
    BACKEND.set(installedBackend);
  }

  static GrapheneBackend backend() {
    GrapheneBackend installedBackend = BACKEND.get();
    if (installedBackend == null) {
      throw new IllegalStateException(
          "Graphene is not installed; the platform bootstrap must run before the API is used");
    }
    return installedBackend;
  }
}
