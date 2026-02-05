package tytoo.grapheneui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The core class of the Graphene library.
 * Devs using the lib must call {@link #init()} in their mod initializer to initialize the library.
 */
public final class GrapheneCore {
    public static final String ID = "graphene-ui";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    private GrapheneCore() {}

    /* Initializes the library. */
    public static void init() {
        LOGGER.info("Graphene initialized!");    
    }
}
