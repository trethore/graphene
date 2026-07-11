package io.github.trethore.graphene.api;

import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.runtime.GrapheneRuntime;

@SuppressWarnings("unused")
public final class Graphene {
  private Graphene() {}

  public static GrapheneContext register(Class<?> anchorClass) {
    return register(anchorClass, GrapheneConfig.defaults());
  }

  public static GrapheneContext register(Class<?> anchorClass, GrapheneConfig config) {
    return GrapheneBackendRegistry.backend().register(anchorClass, config);
  }

  public static GrapheneContext register(String modId) {
    return register(modId, GrapheneConfig.defaults());
  }

  public static GrapheneContext register(String modId, GrapheneConfig config) {
    return GrapheneBackendRegistry.backend().register(modId, config);
  }

  public static GrapheneContext context(Class<?> anchorClass) {
    return GrapheneBackendRegistry.backend().context(anchorClass);
  }

  public static GrapheneContext context(String modId) {
    return GrapheneBackendRegistry.backend().context(modId);
  }

  public static GrapheneGlobalConfig globalConfig() {
    return GrapheneBackendRegistry.backend().globalConfig();
  }

  public static GrapheneRuntime runtime() {
    return GrapheneBackendRegistry.backend().runtime();
  }
}
