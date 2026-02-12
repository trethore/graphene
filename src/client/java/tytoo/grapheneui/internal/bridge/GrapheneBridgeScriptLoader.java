package tytoo.grapheneui.internal.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class GrapheneBridgeScriptLoader {
    private static final String SCRIPT_RESOURCE_PATH = "assets/graphene-ui/bridge/bridge.js";
    private static volatile String script;

    private GrapheneBridgeScriptLoader() {
    }

    static String script() {
        String cachedScript = script;
        if (cachedScript != null) {
            return cachedScript;
        }

        synchronized (GrapheneBridgeScriptLoader.class) {
            if (script != null) {
                return script;
            }

            script = loadScript();
            return script;
        }
    }

    private static String loadScript() {
        ClassLoader classLoader = GrapheneBridgeScriptLoader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(SCRIPT_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bridge script resource: " + SCRIPT_RESOURCE_PATH);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read bridge script resource: " + SCRIPT_RESOURCE_PATH, exception);
        }
    }
}
