package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrapheneFrameBufferTest {
  @Test
  void hasNoFrameBeforeCaptureOrAfterClear() {
    GrapheneFrameBuffer frameBuffer = new GrapheneFrameBuffer();

    assertNull(frameBuffer.latestFrame());
    frameBuffer.capture(
        1, 1, List.of(new BrowserDirtyRegion(0, 0, 1, 1)), ByteBuffer.allocateDirect(4));
    frameBuffer.clear();

    assertNull(frameBuffer.latestFrame());
  }

  @Test
  void capturesIndependentReadOnlyFramesWithIncreasingSequences() {
    GrapheneFrameBuffer frameBuffer = new GrapheneFrameBuffer();
    ByteBuffer pixels = ByteBuffer.allocateDirect(8);
    pixels.put(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).flip();

    BrowserFrame first =
        frameBuffer.capture(2, 1, List.of(new BrowserDirtyRegion(0, 0, 2, 1)), pixels);
    pixels.put(0, (byte) 9);
    BrowserFrame second = frameBuffer.capture(2, 1, List.of(), pixels);

    assertEquals(1, first.sequence());
    assertEquals(2, second.sequence());
    assertEquals(1, first.pixels().get(0));
    assertEquals(9, second.pixels().get(0));
    assertTrue(first.pixels().isReadOnly());
    assertEquals(List.of(new BrowserDirtyRegion(0, 0, 2, 1)), second.dirtyRegions());
    assertEquals(second, frameBuffer.latestFrame());
  }

  @Test
  void compositesPopupFramesAndRestoresMainFrameWhenClosed() {
    GrapheneFrameBuffer frameBuffer = new GrapheneFrameBuffer();
    ByteBuffer main = ByteBuffer.allocateDirect(16);
    main.put(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}).flip();
    frameBuffer.capture(2, 2, List.of(new BrowserDirtyRegion(0, 0, 2, 2)), main);

    frameBuffer.setPopupBounds(new BrowserDirtyRegion(1, 0, 1, 1));
    ByteBuffer popup = ByteBuffer.allocateDirect(4);
    popup.put(new byte[] {20, 21, 22, 23}).flip();
    BrowserFrame composited = frameBuffer.capturePopup(1, 1, popup);

    assertNotNull(composited);
    assertEquals(20, composited.pixels().get(4));
    assertEquals(List.of(new BrowserDirtyRegion(1, 0, 1, 1)), composited.dirtyRegions());

    BrowserFrame restored = frameBuffer.closePopup();

    assertNotNull(restored);
    assertEquals(5, restored.pixels().get(4));
    assertEquals(List.of(new BrowserDirtyRegion(1, 0, 1, 1)), restored.dirtyRegions());
  }

  @Test
  void marksOldAndNewPopupBoundsDirtyWhenPopupMoves() {
    GrapheneFrameBuffer frameBuffer = new GrapheneFrameBuffer();
    ByteBuffer main = ByteBuffer.allocateDirect(12);
    main.put(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}).flip();
    frameBuffer.capture(3, 1, List.of(new BrowserDirtyRegion(0, 0, 3, 1)), main);
    frameBuffer.setPopupBounds(new BrowserDirtyRegion(0, 0, 1, 1));
    ByteBuffer popup = ByteBuffer.allocateDirect(4);
    popup.put(new byte[] {20, 21, 22, 23}).flip();
    frameBuffer.capturePopup(1, 1, popup);

    BrowserFrame moved = frameBuffer.setPopupBounds(new BrowserDirtyRegion(2, 0, 1, 1));

    assertNotNull(moved);
    assertEquals(3, moved.sequence());
    assertEquals(
        List.of(new BrowserDirtyRegion(0, 0, 1, 1), new BrowserDirtyRegion(2, 0, 1, 1)),
        moved.dirtyRegions());
    assertEquals(1, moved.pixels().get(0));
    assertEquals(20, moved.pixels().get(8));
  }

  @Test
  void clipsDirtyRegionsAndMarksResizedFramesFullyDirty() {
    GrapheneFrameBuffer frameBuffer = new GrapheneFrameBuffer();
    ByteBuffer firstPixels = ByteBuffer.allocateDirect(16);
    frameBuffer.capture(2, 2, List.of(new BrowserDirtyRegion(0, 0, 2, 2)), firstPixels);
    ByteBuffer secondPixels = ByteBuffer.allocateDirect(16);

    BrowserFrame clipped =
        frameBuffer.capture(2, 2, List.of(new BrowserDirtyRegion(1, 1, 3, 3)), secondPixels);
    BrowserFrame resized =
        frameBuffer.capture(
            1, 1, List.of(new BrowserDirtyRegion(0, 0, 1, 1)), ByteBuffer.allocateDirect(4));

    assertEquals(List.of(new BrowserDirtyRegion(1, 1, 1, 1)), clipped.dirtyRegions());
    assertEquals(List.of(new BrowserDirtyRegion(0, 0, 1, 1)), resized.dirtyRegions());
  }
}
