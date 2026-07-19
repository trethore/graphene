package io.github.trethore.graphene.debug;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

final class GrapheneDebugKeyBindings {
  private static final KeyMapping.Category CATEGORY =
      KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GrapheneDebugClient.ID, "main"));
  private static final KeyMapping OPEN_BROWSER =
      new KeyMapping(
          "key.grapheneui-debug.open_browser",
          InputConstants.Type.KEYSYM,
          GLFW.GLFW_KEY_F10,
          CATEGORY);
  private static boolean registered;

  private GrapheneDebugKeyBindings() {}

  static void register() {
    if (registered) {
      return;
    }
    registered = true;
    KeyBindingHelper.registerKeyBinding(OPEN_BROWSER);
    ClientTickEvents.END_CLIENT_TICK.register(GrapheneDebugKeyBindings::onClientTick);
  }

  private static void onClientTick(Minecraft minecraft) {
    while (OPEN_BROWSER.consumeClick()) {
      minecraft.setScreen(GrapheneBrowserDebugScreen.instance());
    }
  }
}
