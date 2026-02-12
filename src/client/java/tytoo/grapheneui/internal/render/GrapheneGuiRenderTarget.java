package tytoo.grapheneui.internal.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import tytoo.grapheneui.api.render.GrapheneRenderTarget;
import tytoo.grapheneui.internal.mc.McGuiRender;

import java.util.Objects;

public final class GrapheneGuiRenderTarget implements GrapheneRenderTarget {
    private final GuiGraphics guiGraphics;

    private GrapheneGuiRenderTarget(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public static GrapheneGuiRenderTarget of(GuiGraphics guiGraphics) {
        return new GrapheneGuiRenderTarget(Objects.requireNonNull(guiGraphics, "guiGraphics"));
    }

    @Override
    public void blitTextureRegion(
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
        McGuiRender.blitTextureRegion(
                guiGraphics,
                textureId,
                x,
                y,
                width,
                height,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight,
                textureWidth,
                textureHeight
        );
    }
}
