package tytoo.grapheneui.mc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class McGuiRender {
    private McGuiRender() {
    }

    public static void blitTexture(
            GuiGraphics guiGraphics,
            Identifier textureId,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                textureId,
                x,
                y,
                0.0F,
                0.0F,
                width,
                height,
                textureWidth,
                textureHeight,
                textureWidth,
                textureHeight
        );
    }
}
