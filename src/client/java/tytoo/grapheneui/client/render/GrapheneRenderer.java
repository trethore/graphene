package tytoo.grapheneui.client.render;

import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface GrapheneRenderer {
    void render(GuiGraphics guiGraphics, int x, int y, int width, int height);

    void destroy();

    void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender);

    void onPopupSize(Rectangle rect);

    void onPopupClosed();

    CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution);

    default void onTitleChange(String title) {
    }
}
