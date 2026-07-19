package io.github.trethore.graphene.api.browser;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * An immutable, self-contained CPU snapshot of the composited browser view.
 *
 * <p>Pixels use {@link BrowserPixelFormat#BGRA_8888_PREMULTIPLIED_SRGB}, begin at the upper-left
 * corner, proceed left-to-right and top-to-bottom, and use tightly packed rows. Dirty regions are
 * relative to the immediately preceding frame sequence. Consumers may apply them as partial updates
 * only when they hold sequence {@code sequence() - 1} with matching dimensions; otherwise they must
 * consume the complete pixel buffer.
 */
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
    List<BrowserDirtyRegion> validatedDirtyRegions =
        validateDirtyRegions(width, height, dirtyRegions);
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
    this.dirtyRegions = validatedDirtyRegions;
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

  public BrowserPixelFormat pixelFormat() {
    return BrowserPixelFormat.BGRA_8888_PREMULTIPLIED_SRGB;
  }

  public int rowStrideBytes() {
    return Math.multiplyExact(width, 4);
  }

  /** Returns a read-only buffer positioned at zero with one complete tightly packed frame. */
  public ByteBuffer pixels() {
    return pixels.asReadOnlyBuffer();
  }

  private static List<BrowserDirtyRegion> validateDirtyRegions(
      int width, int height, List<BrowserDirtyRegion> dirtyRegions) {
    List<BrowserDirtyRegion> validatedDirtyRegions =
        List.copyOf(Objects.requireNonNull(dirtyRegions, "dirtyRegions"));
    if (validatedDirtyRegions.isEmpty()) {
      throw new IllegalArgumentException("Frame dirty regions must not be empty");
    }
    for (BrowserDirtyRegion dirtyRegion : validatedDirtyRegions) {
      if (dirtyRegion.x() >= width
          || dirtyRegion.y() >= height
          || dirtyRegion.width() > width - dirtyRegion.x()
          || dirtyRegion.height() > height - dirtyRegion.y()) {
        throw new IllegalArgumentException("Frame dirty region exceeds its dimensions");
      }
    }
    return validatedDirtyRegions;
  }
}
