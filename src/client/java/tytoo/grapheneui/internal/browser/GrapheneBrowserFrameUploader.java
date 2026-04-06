package tytoo.grapheneui.internal.browser;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

final class GrapheneBrowserFrameUploader {
    private static final int BYTES_PER_PIXEL = NativeImage.Format.RGBA.components();
    private static final int DIRTY_RECT_MAX_PARTIAL_UPLOADS = 64;
    private static final double DIRTY_RECT_FULL_UPLOAD_THRESHOLD = 0.45D;

    private final boolean transparent;
    private ByteBuffer uploadScratch;

    GrapheneBrowserFrameUploader(boolean transparent) {
        this.transparent = transparent;
    }

    private static Rectangle clampDirtyRect(Rectangle dirtyRect, int frameWidth, int frameHeight) {
        if (dirtyRect == null || dirtyRect.width <= 0 || dirtyRect.height <= 0) {
            return null;
        }

        return GrapheneBrowserRenderBounds.intersect(
                dirtyRect.x,
                dirtyRect.y,
                dirtyRect.width,
                dirtyRect.height,
                0,
                0,
                frameWidth,
                frameHeight
        );
    }

    void uploadIfNeeded(GrapheneBrowserGpuTexture texture, GraphenePaintBuffer.UploadView frame) {
        if (texture.isUploaded(frame.frameVersion())) {
            return;
        }

        if (frame.fullReRender() || shouldUploadFullFrame(frame.dirtyRects(), frame.width(), frame.height())) {
            uploadFullFrame(texture.texture(), frame.buffer(), frame.width(), frame.height());
        } else {
            uploadDirtyRects(texture.texture(), frame.buffer(), frame.dirtyRects(), frame.width(), frame.height());
        }

        texture.markUploaded(frame.frameVersion());
    }

    CompletableFuture<BufferedImage> createScreenshot(GraphenePaintBuffer.Snapshot snapshot) {
        GraphenePaintBuffer.FrameView mainFrame = snapshot.mainFrame();
        GraphenePaintBuffer.PopupFrameView popupFrame = snapshot.popupFrame();
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
            if (visiblePopupRect != null) {
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
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return CompletableFuture.completedFuture(image);
    }

    void close() {
        uploadScratch = null;
    }

    private boolean shouldUploadFullFrame(Rectangle[] dirtyRects, int frameWidth, int frameHeight) {
        if (dirtyRects == null || dirtyRects.length == 0) {
            return true;
        }

        if (dirtyRects.length >= DIRTY_RECT_MAX_PARTIAL_UPLOADS) {
            return true;
        }

        long fullFramePixels = (long) frameWidth * frameHeight;
        if (fullFramePixels <= 0L) {
            return true;
        }

        long dirtyPixels = 0L;
        for (Rectangle dirtyRect : dirtyRects) {
            Rectangle clampedRect = clampDirtyRect(dirtyRect, frameWidth, frameHeight);
            if (clampedRect != null) {
                dirtyPixels += (long) clampedRect.width * clampedRect.height;
                if ((double) dirtyPixels / fullFramePixels >= DIRTY_RECT_FULL_UPLOAD_THRESHOLD) {
                    return true;
                }
            }
        }

        return dirtyPixels <= 0L;
    }

    private void uploadFullFrame(GpuTexture texture, ByteBuffer buffer, int frameWidth, int frameHeight) {
        int frameBytes = frameWidth * frameHeight * BYTES_PER_PIXEL;
        ByteBuffer converted = ensureScratch(frameBytes);
        converted.clear();

        ByteBuffer source = buffer.duplicate();
        source.position(0);
        source.limit(frameBytes);
        while (source.hasRemaining()) {
            writePixel(converted, source.get(), source.get(), source.get(), source.get());
        }

        converted.flip();
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                texture,
                converted,
                NativeImage.Format.RGBA,
                0,
                0,
                0,
                0,
                frameWidth,
                frameHeight
        );
    }

    private void uploadDirtyRects(GpuTexture texture, ByteBuffer buffer, Rectangle[] dirtyRects, int frameWidth, int frameHeight) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (Rectangle dirtyRect : dirtyRects) {
            Rectangle clampedRect = clampDirtyRect(dirtyRect, frameWidth, frameHeight);
            if (clampedRect != null) {
                ByteBuffer converted = ensureScratch(clampedRect.width * clampedRect.height * BYTES_PER_PIXEL);
                converted.clear();
                convertRegion(buffer, frameWidth, clampedRect.x, clampedRect.y, clampedRect.width, clampedRect.height, converted);
                converted.flip();
                encoder.writeToTexture(
                        texture,
                        converted,
                        NativeImage.Format.RGBA,
                        0,
                        0,
                        clampedRect.x,
                        clampedRect.y,
                        clampedRect.width,
                        clampedRect.height
                );
            }
        }
    }

    private ByteBuffer ensureScratch(int size) {
        if (uploadScratch == null || uploadScratch.capacity() < size) {
            uploadScratch = ByteBuffer.allocateDirect(size);
        }

        uploadScratch.limit(size);
        uploadScratch.position(0);
        return uploadScratch;
    }

    private void convertRegion(ByteBuffer sourceBuffer, int sourceStride, int x, int y, int width, int height, ByteBuffer targetBuffer) {
        for (int row = 0; row < height; row++) {
            int sourceIndex = ((y + row) * sourceStride + x) * BYTES_PER_PIXEL;
            for (int column = 0; column < width; column++) {
                writePixel(
                        targetBuffer,
                        sourceBuffer.get(sourceIndex),
                        sourceBuffer.get(sourceIndex + 1),
                        sourceBuffer.get(sourceIndex + 2),
                        sourceBuffer.get(sourceIndex + 3)
                );
                sourceIndex += BYTES_PER_PIXEL;
            }
        }
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

    private void writePixel(ByteBuffer targetBuffer, byte blue, byte green, byte red, byte sourceAlpha) {
        byte alpha = transparent ? sourceAlpha : (byte) 0xFF;
        targetBuffer.put(red);
        targetBuffer.put(green);
        targetBuffer.put(blue);
        targetBuffer.put(alpha);
    }
}
