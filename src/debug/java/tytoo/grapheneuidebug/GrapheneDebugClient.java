package tytoo.grapheneuidebug;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.GrapheneCore;

public class GrapheneDebugClient implements ClientModInitializer {
    public static final String ID = "graphene-ui-debug";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    /* Debug entry point for testing purposes. */
    @Override
    public void onInitializeClient() {
        GrapheneCore.init();
        LOGGER.info("Hello developer!");
    }
}
