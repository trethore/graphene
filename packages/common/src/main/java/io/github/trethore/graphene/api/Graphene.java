package io.github.trethore.graphene.api;

import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.runtime.GrapheneRuntime;
import io.github.trethore.graphene.internal.runtime.GrapheneContextFactory;
import io.github.trethore.graphene.internal.runtime.GrapheneRuntimeController;

/**
 * Entry point for registering Graphene consumers and accessing the process-wide runtime.
 * Registration and runtime lifecycle are coordinated by the installed platform integration.
 */
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

  /** Registers the mod containing the anchor class with default configuration. */
  public static GrapheneContext register(Class<?> anchorClass) {
    return register(anchorClass, GrapheneConfig.defaults());
  }

  /** Registers the mod containing the anchor class with its consumer configuration. */
  public static GrapheneContext register(Class<?> anchorClass, GrapheneConfig config) {
    return RUNTIME_CONTROLLER.register(anchorClass, config, CONTEXT_FACTORY);
  }

  /** Registers the loaded mod with the given ID using default configuration. */
  public static GrapheneContext register(String modId) {
    return register(modId, GrapheneConfig.defaults());
  }

  /** Registers the loaded mod with the given ID and consumer configuration. */
  public static GrapheneContext register(String modId, GrapheneConfig config) {
    return RUNTIME_CONTROLLER.register(modId, config, CONTEXT_FACTORY);
  }

  /** Returns the registered context for the mod containing the anchor class. */
  public static GrapheneContext context(Class<?> anchorClass) {
    return RUNTIME_CONTROLLER.context(anchorClass);
  }

  /** Returns the registered context for the given mod ID. */
  public static GrapheneContext context(String modId) {
    return RUNTIME_CONTROLLER.context(modId);
  }

  /** Returns the effective process-wide configuration resolved from registered consumers. */
  public static GrapheneGlobalConfig globalConfig() {
    return RUNTIME_CONTROLLER.globalConfig();
  }

  /** Returns the process-wide, read-only runtime view. */
  public static GrapheneRuntime runtime() {
    return RUNTIME_CONTROLLER.runtime();
  }
}
