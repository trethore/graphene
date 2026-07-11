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
}
