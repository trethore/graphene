package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

final class GrapheneBrowserGpuTexture implements AutoCloseable {
  private GpuTexture texture;
  private GpuTextureView view;
  private long uploadedSequence = Long.MIN_VALUE;

  void ensureSize(int width, int height) {
    if (texture != null && texture.getWidth(0) == width && texture.getHeight(0) == height) {
      return;
    }
    close();
    texture =
        RenderSystem.getDevice()
            .createTexture(
                () -> "Graphene Browser",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM,
                width,
                height,
                1,
                1);
    view = RenderSystem.getDevice().createTextureView(texture);
  }

  GpuTexture texture() {
    return texture;
  }

  GpuTextureView view() {
    return view;
  }

  boolean isUploaded(long sequence) {
    return uploadedSequence == sequence;
  }

  boolean canApplyDirtyRegions(long sequence) {
    return uploadedSequence != Long.MIN_VALUE && sequence == uploadedSequence + 1;
  }

  void markUploaded(long sequence) {
    uploadedSequence = sequence;
  }

  @Override
  public void close() {
    if (view != null) {
      view.close();
      view = null;
    }
    if (texture != null) {
      texture.close();
      texture = null;
    }
    uploadedSequence = Long.MIN_VALUE;
  }
}
