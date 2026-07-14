package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GrapheneCefBrowserOptionsTest {
  @Test
  void defaultOptionsRequireNoDevToolsCommands() {
    assertFalse(GrapheneCefBrowserOptions.requiresInitialization(BrowserOptions.defaults()));
    assertTrue(GrapheneCefBrowserOptions.commands(BrowserOptions.defaults()).isEmpty());
  }

  @Test
  void mapsOpaqueBackgroundColorToRgbOverride() {
    BrowserOptions options =
        BrowserOptions.builder().transparent(false).backgroundColor(0x123456).build();

    assertTrue(GrapheneCefBrowserOptions.requiresInitialization(options));
    List<GrapheneCefBrowserOptions.DevToolsCommand> commands =
        GrapheneCefBrowserOptions.commands(options);

    assertEquals(1, commands.size());
    GrapheneCefBrowserOptions.DevToolsCommand command = commands.getFirst();
    assertEquals("Emulation.setDefaultBackgroundColorOverride", command.method());
    JsonObject color =
        JsonParser.parseString(command.parameters()).getAsJsonObject().getAsJsonObject("color");
    assertEquals(0x12, color.get("r").getAsInt());
    assertEquals(0x34, color.get("g").getAsInt());
    assertEquals(0x56, color.get("b").getAsInt());
    assertEquals(1, color.get("a").getAsInt());
  }

  @Test
  void mapsDisabledJavaScriptToScriptExecutionOverride() {
    BrowserOptions options = BrowserOptions.builder().javascriptEnabled(false).build();

    assertTrue(GrapheneCefBrowserOptions.requiresInitialization(options));
    List<GrapheneCefBrowserOptions.DevToolsCommand> commands =
        GrapheneCefBrowserOptions.commands(options);

    assertEquals(1, commands.size());
    GrapheneCefBrowserOptions.DevToolsCommand command = commands.getFirst();
    assertEquals("Emulation.setScriptExecutionDisabled", command.method());
    JsonObject parameters = JsonParser.parseString(command.parameters()).getAsJsonObject();
    assertTrue(parameters.get("value").getAsBoolean());
  }
}
