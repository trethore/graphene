package io.github.trethore.graphene.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneGuiGraphicsExtractorBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements GrapheneGuiGraphicsExtractorBridge {
  @Invoker("innerBlit")
  protected abstract void graphene$invokeInnerBlit(
      RenderPipeline pipeline,
      GpuTextureView texture,
      GpuSampler sampler,
      int x0,
      int y0,
      int x1,
      int y1,
      float u0,
      float u1,
      float v0,
      float v1,
      int color);

  @Unique @Override
  public void graphene$blit(
      RenderPipeline pipeline,
      GpuTextureView texture,
      GpuSampler sampler,
      int x,
      int y,
      int width,
      int height) {
    graphene$invokeInnerBlit(
        pipeline, texture, sampler, x, y, x + width, y + height, 0.0F, 1.0F, 0.0F, 1.0F, -1);
  }
}
