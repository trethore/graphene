package tytoo.grapheneui.api.render;

import net.minecraft.client.gui.GuiGraphics;
import tytoo.grapheneui.internal.render.GrapheneGuiRenderTarget;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface GrapheneRenderer {
    void render(GrapheneRenderTarget renderTarget, int x, int y, int width, int height);

    default void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        render(GrapheneGuiRenderTarget.of(guiGraphics), x, y, width, height);
    }

    default void renderRegion(
            GrapheneRenderTarget renderTarget,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        render(renderTarget, x, y, width, height);
    }

    default void renderRegion(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        renderRegion(GrapheneGuiRenderTarget.of(guiGraphics), x, y, width, height, sourceX, sourceY, sourceWidth, sourceHeight);
    }

    void destroy();

    void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender);

    void onPopupSize(Rectangle rect);

    void onPopupClosed();

    CompletableFuture<BufferedImage> createScreenshot();

    default void onTitleChange(String title) {
    }
}
