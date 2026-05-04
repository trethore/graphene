package tytoo.grapheneui.internal.browser.render;

import com.mojang.blaze3d.platform.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

final class GrapheneScreenshotRenderer {
    private static final int BYTES_PER_PIXEL = NativeImage.Format.RGBA.components();

    private final boolean transparent;

    GrapheneScreenshotRenderer(boolean transparent) {
        this.transparent = transparent;
    }

    CompletableFuture<BufferedImage> createScreenshot(GraphenePaintSnapshot snapshot) {
        GraphenePaintFrameView mainFrame = snapshot.mainFrame();
        GraphenePopupPaintFrameView popupFrame = snapshot.popupFrame();
        if (mainFrame == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No browser frame available"));
        }

        int width = mainFrame.width();
        int height = mainFrame.height();
        int[] pixels = new int[width * height];
        decodeInto(
                pixels,
                width,
                height,
                0,
                0,
                mainFrame.width(),
                mainFrame.height(),
                mainFrame.buffer(),
                0,
                0,
                mainFrame.width(),
                mainFrame.height()
        );

        if (popupFrame != null) {
            decodePopupFrame(pixels, width, height, popupFrame);
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return CompletableFuture.completedFuture(image);
    }

    private void decodePopupFrame(int[] pixels, int width, int height, GraphenePopupPaintFrameView popupFrame) {
        Rectangle popupRect = popupFrame.popupRect();
        Rectangle visiblePopupRect = GrapheneBrowserRenderBounds.intersect(
                popupRect.x,
                popupRect.y,
                popupRect.width,
                popupRect.height,
                0,
                0,
                width,
                height
        );
        if (visiblePopupRect == null) {
            return;
        }

        decodeInto(
                pixels,
                width,
                height,
                visiblePopupRect.x,
                visiblePopupRect.y,
                popupFrame.width(),
                popupFrame.height(),
                popupFrame.buffer(),
                visiblePopupRect.x - popupRect.x,
                visiblePopupRect.y - popupRect.y,
                visiblePopupRect.width,
                visiblePopupRect.height
        );
    }

    private void decodeInto(
            int[] targetPixels,
            int targetWidth,
            int targetHeight,
            int destinationX,
            int destinationY,
            int sourceStride,
            int sourceHeight,
            ByteBuffer sourceBuffer,
            int sourceX,
            int sourceY,
            int width,
            int height
    ) {
        Rectangle visibleRect = GrapheneBrowserRenderBounds.intersect(destinationX, destinationY, width, height, 0, 0, targetWidth, targetHeight);
        if (visibleRect == null) {
            return;
        }

        int clippedSourceX = sourceX + visibleRect.x - destinationX;
        int clippedSourceY = sourceY + visibleRect.y - destinationY;
        int visibleWidth = Math.min(visibleRect.width, sourceStride - clippedSourceX);
        int visibleHeight = Math.min(visibleRect.height, sourceHeight - clippedSourceY);
        if (visibleWidth <= 0 || visibleHeight <= 0) {
            return;
        }

        for (int row = 0; row < visibleHeight; row++) {
            int sourceIndex = ((clippedSourceY + row) * sourceStride + clippedSourceX) * BYTES_PER_PIXEL;
            int targetIndex = (visibleRect.y + row) * targetWidth + visibleRect.x;
            for (int column = 0; column < visibleWidth; column++) {
                int blue = sourceBuffer.get(sourceIndex) & 0xFF;
                int green = sourceBuffer.get(sourceIndex + 1) & 0xFF;
                int red = sourceBuffer.get(sourceIndex + 2) & 0xFF;
                int alpha = transparent ? sourceBuffer.get(sourceIndex + 3) & 0xFF : 0xFF;
                targetPixels[targetIndex + column] = (alpha << 24) | (red << 16) | (green << 8) | blue;
                sourceIndex += BYTES_PER_PIXEL;
            }
        }
    }
}
