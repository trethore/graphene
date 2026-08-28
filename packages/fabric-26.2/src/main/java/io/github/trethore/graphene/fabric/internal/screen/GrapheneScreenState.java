package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GrapheneScreenState extends AbstractGrapheneScreenState {
    void renderContextMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GrapheneContextMenuOverlay activeMenu = (GrapheneContextMenuOverlay) activeContextMenu();
        if (activeMenu != null) {
            activeMenu.render(graphics, mouseX, mouseY);
        }
    }

    @Override
    GrapheneContextMenuOverlaySupport createContextMenuOverlay(
            BrowserContextMenuPresenter.Request request,
            Font font,
            int anchorX,
            int anchorY,
            int screenWidth,
            int screenHeight) {
        return new GrapheneContextMenuOverlay(request, font, anchorX, anchorY, screenWidth, screenHeight);
    }
}
