package io.github.trethore.graphene.fabric.internal.input;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import io.github.trethore.graphene.fabric.internal.util.MinecraftReferences;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class GrapheneKeyboardHandlerHook {
  private GrapheneKeyboardHandlerHook() {}

  public static boolean handleKeyPress(long windowHandle, int action, KeyEvent event) {
    if (action != GLFW.GLFW_PRESS || windowHandle != MinecraftReferences.windowHandle()) {
      return false;
    }

    Screen screen = MinecraftReferences.screen();
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
}
