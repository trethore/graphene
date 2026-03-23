package tytoo.grapheneuidebug;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneuidebug.key.GrapheneDebugKeyBindings;

public class GrapheneDebugClient implements ClientModInitializer {
    public static final String ID = "graphene-ui-debug";
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneDebugClient.class);

    /* Debug entry point for testing purposes. */
    @Override
    public void onInitializeClient() {
        registerHandle();
        GrapheneDebugKeyBindings.register();
        String debugSelector = System.getProperty("graphene.debug");
        if (debugSelector != null && !debugSelector.isBlank()) {
            LOGGER.info("Graphene debug selector enabled: {}", debugSelector);
        }
        LOGGER.info("Graphene debug client initialized");
    }

    private static void registerHandle() {
        GrapheneCore.register(
                GrapheneDebugClient.class,
                GrapheneConfig.builder()
                        .global(GrapheneGlobalConfig.builder()
                                .allowFileSystemAccess()
                                .build())
                        .build()
        );
    }
}
