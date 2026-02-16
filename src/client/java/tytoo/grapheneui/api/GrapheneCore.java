package tytoo.grapheneui.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.core.GrapheneCoreServices;

import java.util.Objects;

/**
 * The core class of the Graphene library.
 * Devs using the lib must call {@link #init()} or {@link #init(GrapheneConfig)} in their mod initializer.
 */
public final class GrapheneCore {
    public static final String ID = "graphene-ui";
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCore.class);
    private static final GrapheneCoreServices SERVICES = GrapheneCoreServices.get();

    private GrapheneCore() {
    }

    /* Initializes the library. */
    public static synchronized void init() {
        init(GrapheneConfig.defaults());
    }

    public static synchronized void init(GrapheneConfig config) {
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");
        if (SERVICES.runtimeInternal().isInitialized()) {
            LOGGER.warn("GrapheneCefRuntime has already been initialized");
            return;
        }

        SERVICES.runtimeInternal().initialize(validatedConfig);
        LOGGER.info("Graphene initialized");
    }

    @SuppressWarnings("unused")
    public static synchronized boolean isInitialized() {
        return runtime().isInitialized();
    }

    public static void closeOwnedSurfaces(Object owner) {
        SERVICES.surfaceManager().closeOwner(Objects.requireNonNull(owner, "owner"));
    }

    public static GrapheneRuntime runtime() {
        return SERVICES.runtime();
    }
}
