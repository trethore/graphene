package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;

@SuppressWarnings("java:S100")
public interface GrapheneScreenBridge extends GrapheneScreenBridgeSupport {
    default void graphene$renderContextMenu(GrapheneGuiGraphics graphics, int mouseX, int mouseY) {
        graphene$state().renderContextMenu(graphics, mouseX, mouseY);
    }
}
