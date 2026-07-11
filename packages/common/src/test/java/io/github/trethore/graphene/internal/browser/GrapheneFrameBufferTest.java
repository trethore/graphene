package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrapheneFrameBufferTest {
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

    assertEquals(20, composited.pixels().get(4));
    assertEquals(List.of(new BrowserDirtyRegion(1, 0, 1, 1)), composited.dirtyRegions());

    frameBuffer.closePopup();

    assertEquals(5, frameBuffer.latestFrame().pixels().get(4));
    assertEquals(
        List.of(new BrowserDirtyRegion(1, 0, 1, 1)), frameBuffer.latestFrame().dirtyRegions());
  }
}
