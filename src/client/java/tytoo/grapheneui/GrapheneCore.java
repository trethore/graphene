package tytoo.grapheneui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.browser.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.cef.GrapheneCefRuntime;

/**
 * The core class of the Graphene library.
 * Devs using the lib must call {@link #init()} in their mod initializer to initialize the library.
 */
public final class GrapheneCore {
    public static final String ID = "graphene-ui";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static final GrapheneCoreServices SERVICES = new GrapheneCoreServices();

    private GrapheneCore() {
    }

    /* Initializes the library. */
    public static synchronized void init() {
        if (runtime().isInitialized()) {
            LOGGER.warn("GrapheneCefRuntime has already been initialized");
            return;
        }

        runtime().initialize();
        LOGGER.info("Graphene initialized");
    }

    @SuppressWarnings("unused")
    public static synchronized boolean isInitialized() {
        return runtime().isInitialized();
    }

    public static GrapheneBrowserSurfaceManager surfaces() {
        return SERVICES.surfaceManager();
    }

    public static GrapheneCefRuntime runtime() {
        return SERVICES.cefRuntime();
    }
}
