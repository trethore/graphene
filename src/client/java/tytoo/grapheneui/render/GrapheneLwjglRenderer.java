package tytoo.grapheneui.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import tytoo.grapheneui.mc.McGuiRender;
import tytoo.grapheneui.mc.McIdentifiers;

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
        this.texture = new GrapheneTexture(McIdentifiers.id("cef/texture_" + textureCounter++));
    }

    private static boolean shouldCopyFullFrame(boolean resized, boolean completeReRender, Rectangle[] dirtyRects) {
        return resized || completeReRender || dirtyRects == null || dirtyRects.length == 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        McGuiRender.blitTexture(guiGraphics, texture.textureId(), x, y, width, height, viewWidth, viewHeight);
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

        if (shouldCopyFullFrame(resized, completeReRender, dirtyRects)) {
            copyRegion(buffer, nativeImage, 0, 0, width, height, width, height, 0, 0);
            texture.upload();
            return;
        }

        copyDirtyRects(buffer, nativeImage, dirtyRects, width, height);

        texture.upload();
    }

    @Override
    public void onPopupSize(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0) {
            return;
        }

        int x = Math.clamp(rect.x, 0, Math.max(0, viewWidth - rect.width));
        int y = Math.clamp(rect.y, 0, Math.max(0, viewHeight - rect.height));
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

        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        int[] sourcePixels = nativeImage.getPixelsABGR();
        int[] imagePixels = new int[width * height];

        for (int index = 0; index < imagePixels.length; index++) {
            imagePixels[index] = ARGB.fromABGR(sourcePixels[index]);
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, imagePixels, 0, width);

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
        int sourceMaxWidth = sourceStride - sourceX;
        int sourceMaxHeight = sourceHeight - sourceY;
        int targetMaxWidth = targetImage.getWidth() - targetX;
        int targetMaxHeight = targetImage.getHeight() - targetY;

        int maxWidth = Math.min(sourceMaxWidth, targetMaxWidth);
        int maxHeight = Math.min(sourceMaxHeight, targetMaxHeight);

        int clampedWidth = maxWidth > 0 ? Math.clamp(width, 0, maxWidth) : 0;
        int clampedHeight = maxHeight > 0 ? Math.clamp(height, 0, maxHeight) : 0;

        if (clampedWidth <= 0 || clampedHeight <= 0) {
            return;
        }

        if (!transparent) {
            copyOpaqueRegion(sourceBuffer, targetImage, sourceX, sourceY, sourceStride, targetX, targetY, clampedWidth, clampedHeight);
            return;
        }

        copyTransparentRegion(sourceBuffer, targetImage, sourceX, sourceY, sourceStride, targetX, targetY, clampedWidth, clampedHeight);
    }

    private void copyTransparentRegion(
            ByteBuffer sourceBuffer,
            NativeImage targetImage,
            int sourceX,
            int sourceY,
            int sourceStride,
            int targetX,
            int targetY,
            int width,
            int height
    ) {
        for (int row = 0; row < height; row++) {
            int sourceIndex = ((sourceY + row) * sourceStride + sourceX) * 4;
            int targetRow = targetY + row;
            for (int column = 0; column < width; column++) {
                int blue = sourceBuffer.get(sourceIndex) & 0xFF;
                int green = sourceBuffer.get(sourceIndex + 1) & 0xFF;
                int red = sourceBuffer.get(sourceIndex + 2) & 0xFF;
                int alpha = sourceBuffer.get(sourceIndex + 3) & 0xFF;

                int pixelAbgr = (alpha << 24) | (blue << 16) | (green << 8) | red;
                targetImage.setPixelABGR(targetX + column, targetRow, pixelAbgr);
                sourceIndex += 4;
            }
        }
    }

    private void copyOpaqueRegion(
            ByteBuffer sourceBuffer,
            NativeImage targetImage,
            int sourceX,
            int sourceY,
            int sourceStride,
            int targetX,
            int targetY,
            int width,
            int height
    ) {
        for (int row = 0; row < height; row++) {
            int sourceIndex = ((sourceY + row) * sourceStride + sourceX) * 4;
            int targetRow = targetY + row;
            for (int column = 0; column < width; column++) {
                int blue = sourceBuffer.get(sourceIndex) & 0xFF;
                int green = sourceBuffer.get(sourceIndex + 1) & 0xFF;
                int red = sourceBuffer.get(sourceIndex + 2) & 0xFF;

                int pixelAbgr = (0xFF << 24) | (blue << 16) | (green << 8) | red;
                targetImage.setPixelABGR(targetX + column, targetRow, pixelAbgr);
                sourceIndex += 4;
            }
        }
    }

    private void copyDirtyRects(ByteBuffer buffer, NativeImage nativeImage, Rectangle[] dirtyRects, int width, int height) {
        for (Rectangle dirtyRect : dirtyRects) {
            copyRegion(buffer, nativeImage, dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height, width, height, dirtyRect.x, dirtyRect.y);
        }
    }
}
