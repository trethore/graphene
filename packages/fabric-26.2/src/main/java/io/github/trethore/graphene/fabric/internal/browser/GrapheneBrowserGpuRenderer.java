package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public final class GrapheneBrowserGpuRenderer implements AutoCloseable {
  private final GrapheneBrowserGpuTexture texture = new GrapheneBrowserGpuTexture();
  private final GrapheneBrowserFrameUploader uploader = new GrapheneBrowserFrameUploader();

  public void render(
      GuiGraphicsExtractor graphics,
      BrowserFrame frame,
      boolean transparent,
      int x,
      int y,
      int width,
      int height) {
    if (width <= 0 || height <= 0) {
      return;
    }
    texture.ensureSize(frame.width(), frame.height());
    uploader.upload(texture, frame, transparent);
    GrapheneGuiGraphicsExtractorBridge bridge = (GrapheneGuiGraphicsExtractorBridge) graphics;
    bridge.graphene$blit(
        transparent
            ? RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA
            : RenderPipelines.GUI_TEXTURED,
        texture.view(),
        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
        x,
        y,
        width,
        height);
  }

  @Override
  public void close() {
    texture.close();
  }
}
