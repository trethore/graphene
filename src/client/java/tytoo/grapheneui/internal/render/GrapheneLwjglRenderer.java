package tytoo.grapheneui.internal.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;
import tytoo.grapheneui.api.render.GrapheneRenderTarget;
import tytoo.grapheneui.api.render.GrapheneRenderer;
import tytoo.grapheneui.internal.mc.McIdentifiers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public final class GrapheneLwjglRenderer implements GrapheneRenderer {
    private static int textureCounter = 0;

    private final GrapheneTexture texture;
    private final GrapheneLwjglPixelPipeline pixelPipeline;
    private final Rectangle popupRect = new Rectangle(0, 0, 0, 0);
    private boolean destroyed = false;
    private int viewWidth = 0;
    private int viewHeight = 0;

    public GrapheneLwjglRenderer(boolean transparent) {
        this.texture = new GrapheneTexture(McIdentifiers.id("cef/texture_" + textureCounter++));
        this.pixelPipeline = new GrapheneLwjglPixelPipeline(transparent);
    }

    @Override
    public void render(GrapheneRenderTarget renderTarget, int x, int y, int width, int height) {
        renderRegion(renderTarget, x, y, width, height, 0, 0, viewWidth, viewHeight);
    }

    @Override
    public void renderRegion(
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
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        int clampedSourceX = Math.clamp(sourceX, 0, Math.max(0, viewWidth - 1));
        int clampedSourceY = Math.clamp(sourceY, 0, Math.max(0, viewHeight - 1));
        int maxSourceWidth = viewWidth - clampedSourceX;
        int maxSourceHeight = viewHeight - clampedSourceY;

        if (sourceWidth <= 0 || sourceHeight <= 0 || maxSourceWidth <= 0 || maxSourceHeight <= 0) {
            return;
        }

        int clampedSourceWidth = Math.clamp(sourceWidth, 1, maxSourceWidth);
        int clampedSourceHeight = Math.clamp(sourceHeight, 1, maxSourceHeight);

        renderTarget.blitTextureRegion(
                texture.textureId(),
                x,
                y,
                width,
                height,
                clampedSourceX,
                clampedSourceY,
                clampedSourceWidth,
                clampedSourceHeight,
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

        boolean copyFullFrame = GrapheneLwjglPixelPipeline.shouldCopyFullFrame(resized, completeReRender, dirtyRects);
        pixelPipeline.copyFrame(buffer, nativeImage, dirtyRects, width, height, copyFullFrame);

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

        pixelPipeline.copyPopup(popupBuffer, nativeImage, popupRect, popupWidth, popupHeight);
        texture.upload();
    }
}
