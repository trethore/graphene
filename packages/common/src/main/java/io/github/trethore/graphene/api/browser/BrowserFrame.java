package io.github.trethore.graphene.api.browser;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("java:S6206")
public final class BrowserFrame {
  private final int width;
  private final int height;
  private final long sequence;
  private final List<BrowserDirtyRegion> dirtyRegions;
  private final ByteBuffer pixels;

  public BrowserFrame(
      int width,
      int height,
      long sequence,
      List<BrowserDirtyRegion> dirtyRegions,
      ByteBuffer pixels) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Frame dimensions must be positive");
    }
    if (sequence < 1) {
      throw new IllegalArgumentException("Frame sequence must be positive");
    }
    int expectedBytes = Math.multiplyExact(Math.multiplyExact(width, height), 4);
    ByteBuffer source = Objects.requireNonNull(pixels, "pixels").duplicate();
    if (source.remaining() != expectedBytes) {
      throw new IllegalArgumentException("Frame pixel buffer size does not match its dimensions");
    }
    ByteBuffer copy = ByteBuffer.allocateDirect(expectedBytes);
    copy.put(source);
    copy.flip();
    this.width = width;
    this.height = height;
    this.sequence = sequence;
    this.dirtyRegions = List.copyOf(Objects.requireNonNull(dirtyRegions, "dirtyRegions"));
    this.pixels = copy.asReadOnlyBuffer();
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public long sequence() {
    return sequence;
  }

  public List<BrowserDirtyRegion> dirtyRegions() {
    return dirtyRegions;
  }

  public ByteBuffer pixels() {
    return pixels.asReadOnlyBuffer();
  }
}
