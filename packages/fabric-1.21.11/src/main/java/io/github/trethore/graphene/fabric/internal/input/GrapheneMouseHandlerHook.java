package io.github.trethore.graphene.fabric.internal.input;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class GrapheneMouseHandlerHook {
  private GrapheneMouseHandlerHook() {}

  public static boolean handleMouseRelease(Screen screen, MouseButtonEvent event) {
    if (screen.mouseReleased(event)) {
      return true;
    }
    GuiEventListener focused = screen.getFocused();
    return event.button() != 0
        && focused instanceof GrapheneWebViewWidget widget
        && widget.mouseReleased(event);
  }
}
