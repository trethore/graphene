package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;

final class GrapheneBrowserFrameUploader {
  private ByteBuffer uploadBuffer;

  void upload(GrapheneBrowserGpuTexture texture, BrowserFrame frame, boolean transparent) {
    if (texture.isUploaded(frame.sequence())) {
      return;
    }
    int byteCount = Math.multiplyExact(Math.multiplyExact(frame.width(), frame.height()), 4);
    ByteBuffer converted = ensureBuffer(byteCount);
    ByteBuffer source = frame.pixels();
    converted.clear();
    while (source.hasRemaining()) {
      byte blue = source.get();
      byte green = source.get();
      byte red = source.get();
      byte alpha = source.get();
      converted.put(red).put(green).put(blue).put(transparent ? alpha : (byte) 0xFF);
    }
    converted.flip();
    RenderSystem.getDevice()
        .createCommandEncoder()
        .writeToTexture(
            texture.texture(),
            converted,
            NativeImage.Format.RGBA,
            0,
            0,
            0,
            0,
            frame.width(),
            frame.height());
    texture.markUploaded(frame.sequence());
  }

  private ByteBuffer ensureBuffer(int capacity) {
    if (uploadBuffer == null || uploadBuffer.capacity() < capacity) {
      uploadBuffer = ByteBuffer.allocateDirect(capacity);
    }
    uploadBuffer.position(0);
    uploadBuffer.limit(capacity);
    return uploadBuffer;
  }
}
