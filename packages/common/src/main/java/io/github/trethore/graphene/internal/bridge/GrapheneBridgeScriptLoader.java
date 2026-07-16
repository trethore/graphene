package io.github.trethore.graphene.internal.bridge;

import io.github.trethore.graphene.internal.logging.GrapheneDebugLogger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class GrapheneBridgeScriptLoader {
  private static final GrapheneDebugLogger DEBUG_LOGGER =
      GrapheneDebugLogger.of(GrapheneBridgeScriptLoader.class);

  private static final String CLIPBOARD_SCRIPT_RESOURCE_PATH =
      "assets/grapheneui/bridge/clipboard.js";
  private static final List<String> SCRIPT_RESOURCE_PATHS =
      List.of(
          "assets/grapheneui/bridge/bridge.js",
          "assets/grapheneui/file-dialog-routing.js",
          "assets/grapheneui/bridge/navigation.js",
          CLIPBOARD_SCRIPT_RESOURCE_PATH,
          "assets/grapheneui/bridge/mouse.js");
  private static final String CLIPBOARD_SCRIPT =
      loadSingleScript(
          GrapheneBridgeScriptLoader.class.getClassLoader(), CLIPBOARD_SCRIPT_RESOURCE_PATH);
  private static final List<String> SCRIPTS = loadScripts();
  private static final List<String> DOCUMENT_SCRIPTS = List.of(CLIPBOARD_SCRIPT);

  private GrapheneBridgeScriptLoader() {}

  static List<String> scripts() {
    return SCRIPTS;
  }

  static List<String> documentScripts() {
    return DOCUMENT_SCRIPTS;
  }

  private static List<String> loadScripts() {
    ClassLoader classLoader = GrapheneBridgeScriptLoader.class.getClassLoader();
    List<String> loadedScripts = new ArrayList<>(SCRIPT_RESOURCE_PATHS.size());
    for (String scriptResourcePath : SCRIPT_RESOURCE_PATHS) {
      loadedScripts.add(
          CLIPBOARD_SCRIPT_RESOURCE_PATH.equals(scriptResourcePath)
              ? CLIPBOARD_SCRIPT
              : loadSingleScript(classLoader, scriptResourcePath));
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
      throw new IllegalStateException(
          "Failed to read bridge script resource: " + scriptResourcePath, exception);
    }
  }
}
