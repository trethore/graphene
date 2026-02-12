package tytoo.grapheneui.mc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

@SuppressWarnings("unused") // util
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
        blitTextureRegion(guiGraphics, textureId, x, y, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    public static void blitTextureRegion(
            GuiGraphics guiGraphics,
            Identifier textureId,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            int textureWidth,
            int textureHeight
    ) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                textureId,
                x,
                y,
                sourceX,
                sourceY,
                width,
                height,
                sourceWidth,
                sourceHeight,
                textureWidth,
                textureHeight
        );
    }
}
