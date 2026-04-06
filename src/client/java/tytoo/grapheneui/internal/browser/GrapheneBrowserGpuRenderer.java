package tytoo.grapheneui.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

final class GrapheneBrowserGpuRenderer implements AutoCloseable {
    private final GrapheneBrowserGpuTexture mainTexture = new GrapheneBrowserGpuTexture("Graphene Browser Main");
    private final GrapheneBrowserGpuTexture popupTexture = new GrapheneBrowserGpuTexture("Graphene Browser Popup");
    private final GrapheneBrowserFrameUploader frameUploader;

    GrapheneBrowserGpuRenderer(boolean transparent) {
        this.frameUploader = new GrapheneBrowserFrameUploader(transparent);
    }

    void render(
            GuiGraphics guiGraphics,
            GraphenePaintBuffer.Snapshot snapshot,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        GraphenePaintBuffer.FrameView mainFrame = snapshot.mainFrame();
        if (mainFrame == null || width <= 0 || height <= 0) {
            return;
        }

        GrapheneBrowserRenderBounds.Region visibleRegion = GrapheneBrowserRenderBounds.clampRegion(
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight,
                mainFrame.width(),
                mainFrame.height()
        );
        if (visibleRegion == null) {
            return;
        }

        mainTexture.ensureSize(mainFrame.width(), mainFrame.height());
        frameUploader.uploadIfNeeded(mainTexture, mainFrame);
        submitBlit(
                guiGraphics,
                mainTexture,
                x,
                y,
                width,
                height,
                visibleRegion.x(),
                visibleRegion.y(),
                visibleRegion.width(),
                visibleRegion.height(),
                mainFrame.width(),
                mainFrame.height()
        );

        GraphenePaintBuffer.PopupFrameView popupFrame = snapshot.popupFrame();
        if (popupFrame == null) {
            return;
        }

        GrapheneBrowserRenderBounds.PopupPlacement popupPlacement = GrapheneBrowserRenderBounds.placePopup(
                popupFrame.popupRect(),
                visibleRegion,
                x,
                y,
                width,
                height,
                popupFrame.width(),
                popupFrame.height()
        );
        if (popupPlacement == null) {
            return;
        }

        popupTexture.ensureSize(popupFrame.width(), popupFrame.height());
        frameUploader.uploadIfNeeded(popupTexture, popupFrame);
        submitBlit(
                guiGraphics,
                popupTexture,
                popupPlacement.x(),
                popupPlacement.y(),
                popupPlacement.width(),
                popupPlacement.height(),
                popupPlacement.sourceX(),
                popupPlacement.sourceY(),
                popupPlacement.sourceWidth(),
                popupPlacement.sourceHeight(),
                popupFrame.width(),
                popupFrame.height()
        );
    }

    CompletableFuture<BufferedImage> createScreenshot(GraphenePaintBuffer.Snapshot snapshot) {
        return frameUploader.createScreenshot(snapshot);
    }

    @Override
    public void close() {
        mainTexture.close();
        popupTexture.close();
        frameUploader.close();
    }

    private void submitBlit(
            GuiGraphics guiGraphics,
            GrapheneBrowserGpuTexture texture,
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
        float u0 = (float) sourceX / textureWidth;
        float u1 = (float) (sourceX + sourceWidth) / textureWidth;
        float v0 = (float) sourceY / textureHeight;
        float v1 = (float) (sourceY + sourceHeight) / textureHeight;

        guiGraphics.guiRenderState.submitGuiElement(
                new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED,
                        TextureSetup.singleTexture(texture.view(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
                        new Matrix3x2f(guiGraphics.pose()),
                        x,
                        y,
                        x + width,
                        y + height,
                        u0,
                        u1,
                        v0,
                        v1,
                        -1,
                        guiGraphics.scissorStack.peek()
                )
        );
    }
}
