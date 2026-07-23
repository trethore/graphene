package io.github.trethore.graphene.fabric.internal.input;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenBridge;
import io.github.trethore.graphene.fabric.internal.util.MinecraftReferences;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class GrapheneKeyboardHandlerHook {
  private static final Set<Integer> SUPPRESSED_CONTEXT_MENU_KEYS = new HashSet<>();

  private GrapheneKeyboardHandlerHook() {}

  public static boolean handleKeyPress(long windowHandle, int action, KeyEvent event) {
    if (windowHandle != MinecraftReferences.windowHandle()) {
      return false;
    }

    Screen screen = MinecraftReferences.screen();
    if (action == GLFW.GLFW_RELEASE && SUPPRESSED_CONTEXT_MENU_KEYS.remove(event.key())) {
      return true;
    }
    boolean contextMenuKey = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
    if (contextMenuKey
        && screen instanceof GrapheneScreenBridge bridge
        && bridge.graphene$handleContextMenuKey(event)) {
      SUPPRESSED_CONTEXT_MENU_KEYS.add(event.key());
      screen.afterKeyboardAction();
      return true;
    }
    if (action != GLFW.GLFW_PRESS) {
      return false;
    }
    if (screen == null || !(screen.getFocused() instanceof GrapheneWebViewWidget webView)) {
      return false;
    }

    Options options = MinecraftReferences.options();
    if (!options.keyFullscreen.matches(event) && !options.keyScreenshot.matches(event)) {
      return false;
    }

    screen.afterKeyboardAction();
    webView.keyPressed(event);
    return true;
  }

  public static boolean handleCharacterInput(long windowHandle) {
    if (windowHandle != MinecraftReferences.windowHandle()) {
      return false;
    }
    Screen screen = MinecraftReferences.screen();
    return screen instanceof GrapheneScreenBridge bridge && bridge.graphene$isContextMenuOpen();
  }
}
