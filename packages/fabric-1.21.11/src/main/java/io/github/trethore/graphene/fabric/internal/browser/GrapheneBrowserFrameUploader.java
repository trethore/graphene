package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;

final class GrapheneBrowserFrameUploader {
  private static final int MAX_PARTIAL_UPLOADS = 64;
  private static final double FULL_UPLOAD_THRESHOLD = 0.45;
  private ByteBuffer uploadBuffer;

  void upload(GrapheneBrowserGpuTexture texture, BrowserFrame frame, boolean transparent) {
    if (texture.isUploaded(frame.sequence())) {
      return;
    }
    ByteBuffer pixels = frame.pixels();
    int rowStrideBytes = frame.rowStrideBytes();
    if (shouldUploadFullFrame(frame, texture.canApplyDirtyRegions(frame.sequence()))) {
      uploadRegion(
          texture, pixels, rowStrideBytes, transparent, 0, 0, frame.width(), frame.height());
    } else {
      for (BrowserDirtyRegion region : frame.dirtyRegions()) {
        int width = Math.min(region.width(), frame.width() - region.x());
        int height = Math.min(region.height(), frame.height() - region.y());
        if (width > 0 && height > 0) {
          uploadRegion(
              texture, pixels, rowStrideBytes, transparent, region.x(), region.y(), width, height);
        }
      }
    }
    texture.markUploaded(frame.sequence());
  }

  private void uploadRegion(
      GrapheneBrowserGpuTexture texture,
      ByteBuffer source,
      int rowStrideBytes,
      boolean transparent,
      int x,
      int y,
      int width,
      int height) {
    int byteCount = Math.multiplyExact(Math.multiplyExact(width, height), 4);
    ByteBuffer converted = ensureBuffer(byteCount);
    converted.clear();
    for (int row = 0; row < height; row++) {
      int sourceIndex = (y + row) * rowStrideBytes + x * 4;
      for (int column = 0; column < width; column++) {
        byte blue = source.get(sourceIndex++);
        byte green = source.get(sourceIndex++);
        byte red = source.get(sourceIndex++);
        byte alpha = source.get(sourceIndex++);
        converted.put(red).put(green).put(blue).put(transparent ? alpha : (byte) 0xFF);
      }
    }
    converted.flip();
    RenderSystem.getDevice()
        .createCommandEncoder()
        .writeToTexture(
            texture.texture(), converted, NativeImage.Format.RGBA, 0, 0, x, y, width, height);
  }

  static boolean shouldUploadFullFrame(BrowserFrame frame, boolean consecutiveSequence) {
    if (!consecutiveSequence || frame.dirtyRegions().size() >= MAX_PARTIAL_UPLOADS) {
      return true;
    }
    long dirtyPixels = 0;
    long framePixels = (long) frame.width() * frame.height();
    for (BrowserDirtyRegion region : frame.dirtyRegions()) {
      int availableWidth = Math.max(0, frame.width() - region.x());
      int availableHeight = Math.max(0, frame.height() - region.y());
      int width = Math.clamp(region.width(), 0, availableWidth);
      int height = Math.clamp(region.height(), 0, availableHeight);
      dirtyPixels += (long) width * height;
      if ((double) dirtyPixels / framePixels >= FULL_UPLOAD_THRESHOLD) {
        return true;
      }
    }
    return dirtyPixels == 0;
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
