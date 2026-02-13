package tytoo.grapheneui.internal.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class GrapheneBridgeScriptLoader {
    private static final List<String> SCRIPT_RESOURCE_PATHS = List.of(
            "assets/graphene-ui/bridge/bridge.js",
            "assets/graphene-ui/bridge/mouse.js"
    );
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
        StringBuilder scriptBuilder = new StringBuilder();
        for (String scriptResourcePath : SCRIPT_RESOURCE_PATHS) {
            if (!scriptBuilder.isEmpty()) {
                scriptBuilder.append("\n;\n");
            }

            scriptBuilder.append(loadSingleScript(classLoader, scriptResourcePath));
        }

        return scriptBuilder.toString();
    }

    private static String loadSingleScript(ClassLoader classLoader, String scriptResourcePath) {
        try (InputStream inputStream = classLoader.getResourceAsStream(scriptResourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bridge script resource: " + scriptResourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read bridge script resource: " + scriptResourcePath, exception);
        }
    }
}
