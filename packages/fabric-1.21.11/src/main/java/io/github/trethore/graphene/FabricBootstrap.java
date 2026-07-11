package io.github.trethore.graphene;

import io.github.trethore.graphene.fabric.internal.platform.FabricPlatformServices;
import io.github.trethore.graphene.internal.cef.GrapheneCefRuntime;
import io.github.trethore.graphene.internal.platform.GraphenePlatformServices;
import io.github.trethore.graphene.internal.runtime.GrapheneRuntimeController;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricBootstrap implements ClientModInitializer {
  public static final String MOD_ID = "grapheneui";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    GraphenePlatformServices platformServices = FabricPlatformServices.create();
    GrapheneRuntimeController controller = GrapheneRuntimeController.instance();
    controller.install(platformServices);
    controller.installBrowserRuntime(new GrapheneCefRuntime(platformServices.startupPresenter()));
    LOGGER.info("Installed {} platform services", MOD_ID);
  }
}
