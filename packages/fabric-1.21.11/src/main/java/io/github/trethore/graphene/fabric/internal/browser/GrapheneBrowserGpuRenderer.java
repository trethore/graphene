package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

public final class GrapheneBrowserGpuRenderer implements AutoCloseable {
  private final GrapheneBrowserGpuTexture texture = new GrapheneBrowserGpuTexture();
  private final GrapheneBrowserFrameUploader uploader = new GrapheneBrowserFrameUploader();

  public void render(
      GuiGraphics graphics,
      BrowserFrame frame,
      boolean transparent,
      int x,
      int y,
      int width,
      int height) {
    if (frame == null || width <= 0 || height <= 0) {
      return;
    }
    texture.ensureSize(frame.width(), frame.height());
    uploader.upload(texture, frame, transparent);
    graphics.guiRenderState.submitGuiElement(
        new BlitRenderState(
            RenderPipelines.GUI_TEXTURED,
            TextureSetup.singleTexture(
                texture.view(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
            new Matrix3x2f(graphics.pose()),
            x,
            y,
            x + width,
            y + height,
            0.0F,
            1.0F,
            0.0F,
            1.0F,
            -1,
            graphics.scissorStack.peek()));
  }

  @Override
  public void close() {
    texture.close();
  }
}
