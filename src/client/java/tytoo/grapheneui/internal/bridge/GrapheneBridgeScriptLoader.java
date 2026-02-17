package tytoo.grapheneui.internal.bridge;

import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class GrapheneBridgeScriptLoader {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneBridgeScriptLoader.class);

    private static final List<String> SCRIPT_RESOURCE_PATHS = List.of(
            "assets/graphene-ui/bridge/bridge.js",
            "assets/graphene-ui/bridge/mouse.js"
    );
    private static final List<String> SCRIPTS = loadScripts();

    private GrapheneBridgeScriptLoader() {
    }

    static List<String> scripts() {
        return SCRIPTS;
    }

    private static List<String> loadScripts() {
        ClassLoader classLoader = GrapheneBridgeScriptLoader.class.getClassLoader();
        List<String> loadedScripts = new java.util.ArrayList<>(SCRIPT_RESOURCE_PATHS.size());
        for (String scriptResourcePath : SCRIPT_RESOURCE_PATHS) {
            loadedScripts.add(loadSingleScript(classLoader, scriptResourcePath));
        }

        DEBUG_LOGGER.debug("Loaded {} Graphene bridge bootstrap script(s)", loadedScripts.size());

        return List.copyOf(loadedScripts);
    }

    private static String loadSingleScript(ClassLoader classLoader, String scriptResourcePath) {
        try (InputStream inputStream = classLoader.getResourceAsStream(scriptResourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bridge script resource: " + scriptResourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            DEBUG_LOGGER.debug("Failed to read bridge script resource {}", scriptResourcePath, exception);
            throw new IllegalStateException("Failed to read bridge script resource: " + scriptResourcePath, exception);
        }
    }
}
