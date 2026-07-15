package io.github.trethore.graphene.api;

import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.runtime.GrapheneRuntime;
import io.github.trethore.graphene.internal.runtime.GrapheneContextFactory;
import io.github.trethore.graphene.internal.runtime.GrapheneRuntimeController;

@SuppressWarnings("unused")
public final class Graphene {
  private static final GrapheneRuntimeController RUNTIME_CONTROLLER =
      GrapheneRuntimeController.instance();
  private static final GrapheneContextFactory CONTEXT_FACTORY =
      parameters ->
          new GrapheneContext(
              parameters.id(),
              parameters.config(),
              parameters.appAssets(),
              parameters.classpathAssets(),
              parameters.httpAssets(),
              parameters.httpUrlFactory(),
              parameters.browsers());

  private Graphene() {}

  public static GrapheneContext register(Class<?> anchorClass) {
    return register(anchorClass, GrapheneConfig.defaults());
  }

  public static GrapheneContext register(Class<?> anchorClass, GrapheneConfig config) {
    return RUNTIME_CONTROLLER.register(anchorClass, config, CONTEXT_FACTORY);
  }

  public static GrapheneContext register(String modId) {
    return register(modId, GrapheneConfig.defaults());
  }

  public static GrapheneContext register(String modId, GrapheneConfig config) {
    return RUNTIME_CONTROLLER.register(modId, config, CONTEXT_FACTORY);
  }

  public static GrapheneContext context(Class<?> anchorClass) {
    return RUNTIME_CONTROLLER.context(anchorClass);
  }

  public static GrapheneContext context(String modId) {
    return RUNTIME_CONTROLLER.context(modId);
  }

  public static GrapheneGlobalConfig globalConfig() {
    return RUNTIME_CONTROLLER.globalConfig();
  }

  public static GrapheneRuntime runtime() {
    return RUNTIME_CONTROLLER.runtime();
  }
}
