package io.github.trethore.graphene.fabric.internal.input;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenBridge;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class GrapheneMouseHandlerHook {
  private GrapheneMouseHandlerHook() {}

  public static boolean handleMouseClick(
      Screen screen, MouseButtonEvent event, boolean doubleClick) {
    if (screen instanceof GrapheneScreenBridge bridge
        && bridge.graphene$handleContextMenuClick(event)) {
      return true;
    }
    return screen.mouseClicked(event, doubleClick);
  }

  public static boolean handleMouseRelease(Screen screen, MouseButtonEvent event) {
    if (screen instanceof GrapheneScreenBridge bridge
        && bridge.graphene$handleContextMenuRelease(event)) {
      return true;
    }
    if (screen.mouseReleased(event)) {
      return true;
    }
    GuiEventListener focused = screen.getFocused();
    return event.button() != 0
        && focused instanceof GrapheneWebViewWidget widget
        && widget.mouseReleased(event);
  }

  public static boolean handleMouseScroll(
      Screen screen, double mouseX, double mouseY, double horizontal, double vertical) {
    if (screen instanceof GrapheneScreenBridge bridge && bridge.graphene$isContextMenuOpen()) {
      return true;
    }
    return screen.mouseScrolled(mouseX, mouseY, horizontal, vertical);
  }
}
