package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

public interface GrapheneGuiGraphicsExtractorBridge {
  void graphene$blit(
      RenderPipeline pipeline,
      GpuTextureView texture,
      GpuSampler sampler,
      int x,
      int y,
      int width,
      int height);
}
