package tytoo.grapheneui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.cef.GrapheneCefRuntime;

/**
 * The core class of the Graphene library.
 * Devs using the lib must call {@link #init()} in their mod initializer to initialize the library.
 */
public final class GrapheneCore {
    public static final String ID = "graphene-ui";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    private GrapheneCore() {
    }

    /* Initializes the library. */
    public static synchronized void init() {
        if (GrapheneCefRuntime.isInitialized()) {
            LOGGER.warn("GrapheneCefRuntime has already been initialized");
            return;
        }

        GrapheneCefRuntime.initialize();
        LOGGER.info("Graphene initialized");
    }

    @SuppressWarnings("unused")
    public static synchronized boolean isInitialized() {
        return GrapheneCefRuntime.isInitialized();
    }
}
