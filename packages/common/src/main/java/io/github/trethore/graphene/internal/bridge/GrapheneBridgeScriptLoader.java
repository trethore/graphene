package io.github.trethore.graphene.internal.bridge;

import io.github.trethore.graphene.api.browser.input.BrowserKeyPlatform;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneBridgeScriptLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgeScriptLoader.class);

    private static final String CLIPBOARD_SCRIPT_RESOURCE_PATH = "assets/grapheneui/bridge/clipboard.js";
    private static final String FILE_DIALOG_ROUTING_SCRIPT_RESOURCE_PATH = "assets/grapheneui/file-dialog-routing.js";

    private static final String CLIPBOARD_SCRIPT = BrowserKeyPlatform.current() == BrowserKeyPlatform.LINUX
            ? loadSingleScript(GrapheneBridgeScriptLoader.class.getClassLoader(), CLIPBOARD_SCRIPT_RESOURCE_PATH)
            : null;
    private static final String FILE_DIALOG_ROUTING_SCRIPT = loadSingleScript(
            GrapheneBridgeScriptLoader.class.getClassLoader(), FILE_DIALOG_ROUTING_SCRIPT_RESOURCE_PATH);

    private static final List<String> SCRIPTS = loadScripts();
    private static final List<String> DOCUMENT_SCRIPTS = loadDocumentScripts();

    private GrapheneBridgeScriptLoader() {}

    static List<String> scripts() {
        return SCRIPTS;
    }

    static List<String> documentScripts() {
        return DOCUMENT_SCRIPTS;
    }

    private static List<String> loadScripts() {
        ClassLoader classLoader = GrapheneBridgeScriptLoader.class.getClassLoader();
        List<String> loadedScripts = new ArrayList<>(CLIPBOARD_SCRIPT == null ? 4 : 5);
        loadedScripts.add(loadSingleScript(classLoader, "assets/grapheneui/bridge/bridge.js"));
        loadedScripts.add(FILE_DIALOG_ROUTING_SCRIPT);
        loadedScripts.add(loadSingleScript(classLoader, "assets/grapheneui/bridge/navigation.js"));
        if (CLIPBOARD_SCRIPT != null) {
            loadedScripts.add(CLIPBOARD_SCRIPT);
        }
        loadedScripts.add(loadSingleScript(classLoader, "assets/grapheneui/bridge/mouse.js"));

        LOGGER.debug("Loaded {} Graphene bridge bootstrap script(s)", loadedScripts.size());

        return List.copyOf(loadedScripts);
    }

    private static List<String> loadDocumentScripts() {
        return CLIPBOARD_SCRIPT == null
                ? List.of(FILE_DIALOG_ROUTING_SCRIPT)
                : List.of(FILE_DIALOG_ROUTING_SCRIPT, CLIPBOARD_SCRIPT);
    }

    private static String loadSingleScript(ClassLoader classLoader, String scriptResourcePath) {
        try (InputStream inputStream = classLoader.getResourceAsStream(scriptResourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bridge script resource: " + scriptResourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.debug("Failed to read bridge script resource {}", scriptResourcePath, exception);
            throw new IllegalStateException("Failed to read bridge script resource: " + scriptResourcePath, exception);
        }
    }
}
