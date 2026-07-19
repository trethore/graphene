package io.github.trethore.graphene.api.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrowserFrameTest {
  @Test
  void exposesNormalizedPixelContract() {
    BrowserFrame frame = frame(2, List.of(new BrowserDirtyRegion(0, 0, 2, 1)));

    assertEquals(BrowserPixelFormat.BGRA_8888_PREMULTIPLIED_SRGB, frame.pixelFormat());
    assertEquals(8, frame.rowStrideBytes());
    assertEquals(0, frame.pixels().position());
    assertEquals(8, frame.pixels().limit());
    assertTrue(frame.pixels().isReadOnly());
  }

  @Test
  void rejectsMissingOrOutOfBoundsDirtyRegions() {
    List<BrowserDirtyRegion> emptyDirtyRegions = List.of();
    BrowserDirtyRegion outOfBoundsDirtyRegion = new BrowserDirtyRegion(0, 0, 2, 1);
    List<BrowserDirtyRegion> outOfBoundsDirtyRegions = List.of(outOfBoundsDirtyRegion);

    assertThrows(IllegalArgumentException.class, () -> frame(1, emptyDirtyRegions));
    assertThrows(IllegalArgumentException.class, () -> frame(1, outOfBoundsDirtyRegions));
  }

  private static BrowserFrame frame(int width, List<BrowserDirtyRegion> dirtyRegions) {
    return new BrowserFrame(width, 1, 1, dirtyRegions, ByteBuffer.allocateDirect(width * 4));
  }
}
