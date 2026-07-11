package io.github.trethore.graphene.debug;

import io.github.trethore.graphene.api.Graphene;
import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.config.GrapheneRemoteDebugConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneDebugClient implements ClientModInitializer {
  public static final String ID = "grapheneui-debug";
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneDebugClient.class);
  private static GrapheneContext context;

  public static GrapheneContext context() {
    if (context == null) {
      throw new IllegalStateException("Graphene debug client has not been initialized");
    }
    return context;
  }

  @Override
  public void onInitializeClient() {
    initialize();
  }

  private static void initialize() {
    context =
        Graphene.register(
            ID,
            GrapheneConfig.builder()
                .global(
                    GrapheneGlobalConfig.builder()
                        .allowBrowserFileAccess()
                        .remoteDebugging(GrapheneRemoteDebugConfig.builder().randomPort().build())
                        .build())
                .build());
    GrapheneDebugKeyBindings.register();
    LOGGER.info("Graphene debug client initialized");
  }
}
