package io.github.trethore.graphene;

import io.github.trethore.graphene.fabric.internal.platform.FabricPlatformServices;
import io.github.trethore.graphene.internal.runtime.GrapheneRuntimeController;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricBootstrap implements ClientModInitializer {
  public static final String MOD_ID = "grapheneui";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    GrapheneRuntimeController.instance().install(FabricPlatformServices.create());
    LOGGER.info("Installed {} platform services", MOD_ID);
  }
}
