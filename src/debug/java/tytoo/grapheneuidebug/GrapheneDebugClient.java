package tytoo.grapheneuidebug;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneuidebug.key.GrapheneDebugKeyBindings;

public class GrapheneDebugClient implements ClientModInitializer {
    public static final String ID = "graphene-ui-debug";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    /* Debug entry point for testing purposes. */
    @Override
    public void onInitializeClient() {
        GrapheneCore.init();
        GrapheneDebugKeyBindings.register();
        LOGGER.info("Graphene debug client initialized");
    }
}
