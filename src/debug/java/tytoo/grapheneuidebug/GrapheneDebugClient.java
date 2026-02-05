package tytoo.grapheneuidebug;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrapheneDebugClient implements ClientModInitializer {
    public static final String MOD_ID = "assets/graphene-ui-debug";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Graphene UI Debug mod initialized — ready for e2e testing");
    }
}
