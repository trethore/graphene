package tytoo.grapheneui.internal.render;

import com.mojang.blaze3d.platform.NativeImage;

import java.awt.*;
import java.nio.ByteBuffer;

final class GrapheneLwjglPixelPipeline {
    private final boolean transparent;

    GrapheneLwjglPixelPipeline(boolean transparent) {
        this.transparent = transparent;
    }

    static boolean shouldCopyFullFrame(boolean resized, boolean completeReRender, Rectangle[] dirtyRects) {
        return resized || completeReRender || dirtyRects == null || dirtyRects.length == 0;
    }

    void copyFrame(
            ByteBuffer sourceBuffer,
            NativeImage targetImage,
            Rectangle[] dirtyRects,
            int width,
            int height,
            boolean copyFullFrame
    ) {
        if (copyFullFrame) {
            copyRegion(sourceBuffer, targetImage, 0, 0, width, height, width, height, 0, 0);
            return;
        }

        for (Rectangle dirtyRect : dirtyRects) {
            copyRegion(
                    sourceBuffer,
                    targetImage,
                    dirtyRect.x,
                    dirtyRect.y,
                    dirtyRect.width,
                    dirtyRect.height,
                    width,
                    height,
                    dirtyRect.x,
                    dirtyRect.y
            );
        }
    }

    void copyPopup(ByteBuffer popupBuffer, NativeImage targetImage, Rectangle popupRect, int popupWidth, int popupHeight) {
        copyRegion(
                popupBuffer,
                targetImage,
                0,
                0,
                popupWidth,
                popupHeight,
                popupWidth,
                popupHeight,
                popupRect.x,
                popupRect.y
        );
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
}
