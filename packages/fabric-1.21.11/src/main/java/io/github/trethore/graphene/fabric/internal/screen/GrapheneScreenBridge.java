package io.github.trethore.graphene.fabric.internal.screen;

import net.minecraft.client.gui.GuiGraphics;

@SuppressWarnings("java:S100")
public interface GrapheneScreenBridge extends GrapheneScreenBridgeSupport {
    default void graphene$renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        graphene$state().renderContextMenu(graphics, mouseX, mouseY);
    }
}
