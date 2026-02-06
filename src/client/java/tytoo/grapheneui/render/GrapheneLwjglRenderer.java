package tytoo.grapheneui.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import tytoo.grapheneui.GrapheneCore;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public final class GrapheneLwjglRenderer implements GrapheneRenderer {
    private static int textureCounter = 0;

    private final GrapheneTexture texture;
    private final boolean transparent;
    private final Rectangle popupRect = new Rectangle(0, 0, 0, 0);
    private boolean destroyed = false;
    private int viewWidth = 0;
    private int viewHeight = 0;

    public GrapheneLwjglRenderer(boolean transparent) {
        this.transparent = transparent;
        Identifier textureId = Identifier.fromNamespaceAndPath(GrapheneCore.ID, "cef/texture_" + textureCounter++);
        this.texture = new GrapheneTexture(textureId);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture.textureId(),
                x,
                y,
                0.0F,
                0.0F,
                width,
                height,
                viewWidth,
                viewHeight,
                viewWidth,
                viewHeight
        );
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }

        destroyed = true;
        texture.release();
    }

    @Override
    public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender) {
        if (destroyed) {
            return;
        }

        if (popup) {
            uploadPopup(buffer, width, height);
            return;
        }

        boolean resized = texture.ensureSize(width, height);
        NativeImage nativeImage = texture.getPixels();
        if (nativeImage == null) {
            return;
        }

        viewWidth = width;
        viewHeight = height;

        if (resized || completeReRender || dirtyRects == null || dirtyRects.length == 0) {
            copyRegion(buffer, nativeImage, 0, 0, width, height, width, height, 0, 0);
            texture.upload();
            return;
        }

        for (Rectangle dirtyRect : dirtyRects) {
            copyRegion(buffer, nativeImage, dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height, width, height, dirtyRect.x, dirtyRect.y);
        }

        texture.upload();
    }

    @Override
    public void onPopupSize(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0) {
            return;
        }

        int x = Math.max(0, Math.min(rect.x, Math.max(0, viewWidth - rect.width)));
        int y = Math.max(0, Math.min(rect.y, Math.max(0, viewHeight - rect.height)));
        popupRect.setBounds(x, y, rect.width, rect.height);
    }

    @Override
    public void onPopupClosed() {
        popupRect.setBounds(0, 0, 0, 0);
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        NativeImage nativeImage = texture.getPixels();
        if (nativeImage == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No browser frame available"));
        }

        BufferedImage image = new BufferedImage(nativeImage.getWidth(), nativeImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] pixels = nativeImage.getPixelsABGR();
        for (int y = 0; y < nativeImage.getHeight(); y++) {
            for (int x = 0; x < nativeImage.getWidth(); x++) {
                int pixelAbgr = pixels[y * nativeImage.getWidth() + x];
                image.setRGB(x, y, ARGB.fromABGR(pixelAbgr));
            }
        }

        return CompletableFuture.completedFuture(image);
    }

    private void uploadPopup(ByteBuffer popupBuffer, int popupWidth, int popupHeight) {
        if (popupRect.width <= 0 || popupRect.height <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        NativeImage nativeImage = texture.getPixels();
        if (nativeImage == null) {
            return;
        }

        copyRegion(
                popupBuffer,
                nativeImage,
                0,
                0,
                popupWidth,
                popupHeight,
                popupWidth,
                popupHeight,
                popupRect.x,
                popupRect.y
        );
        texture.upload();
    }

    private void copyRegion(
            ByteBuffer sourceBuffer,
            NativeImage targetImage,
            int sourceX,
            int sourceY,
            int width,
            int height,
            int sourceStride,
            int sourceHeight,
            int targetX,
            int targetY
    ) {
        int maxWidth = Math.min(width, sourceStride - sourceX);
        int maxHeight = Math.min(height, sourceHeight - sourceY);

        int clampedWidth = Math.min(maxWidth, targetImage.getWidth() - targetX);
        int clampedHeight = Math.min(maxHeight, targetImage.getHeight() - targetY);

        if (clampedWidth <= 0 || clampedHeight <= 0) {
            return;
        }

        for (int row = 0; row < clampedHeight; row++) {
            int sourceRow = sourceY + row;
            int targetRow = targetY + row;
            for (int column = 0; column < clampedWidth; column++) {
                int sourceColumn = sourceX + column;
                int sourceIndex = (sourceRow * sourceStride + sourceColumn) * 4;

                int blue = sourceBuffer.get(sourceIndex) & 0xFF;
                int green = sourceBuffer.get(sourceIndex + 1) & 0xFF;
                int red = sourceBuffer.get(sourceIndex + 2) & 0xFF;
                int alpha = sourceBuffer.get(sourceIndex + 3) & 0xFF;

                if (!transparent) {
                    alpha = 0xFF;
                }

                int pixelAbgr = (alpha << 24) | (blue << 16) | (green << 8) | red;
                targetImage.setPixelABGR(targetX + column, targetRow, pixelAbgr);
            }
        }
    }
}
