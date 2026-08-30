package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GrapheneFramePixelConverterTest {
    @Test
    void convertsBgraPixelsToRgba() {
        ByteBuffer pixels = ByteBuffer.allocateDirect(8);
        pixels.put(new byte[] {1, 2, 3, (byte) 255, 5, 6, 7, (byte) 255}).flip();
        BrowserFrame frame = new BrowserFrame(2, 1, 1, List.of(new BrowserDirtyRegion(0, 0, 2, 1)), pixels);

        ByteBuffer converted =
                new GrapheneFramePixelConverter().convert(frame, new BrowserDirtyRegion(0, 0, 2, 1), true);

        byte[] result = new byte[converted.remaining()];
        converted.get(result);
        assertArrayEquals(new byte[] {3, 2, 1, (byte) 255, 7, 6, 5, (byte) 255}, result);
    }

    @Test
    void unpremultipliesTransparentPixels() {
        ByteBuffer pixels = ByteBuffer.allocateDirect(4);
        pixels.put(new byte[] {25, 50, 100, (byte) 128}).flip();
        BrowserFrame frame = new BrowserFrame(1, 1, 1, List.of(new BrowserDirtyRegion(0, 0, 1, 1)), pixels);

        ByteBuffer converted =
                new GrapheneFramePixelConverter().convert(frame, new BrowserDirtyRegion(0, 0, 1, 1), true);

        byte[] result = new byte[converted.remaining()];
        converted.get(result);
        assertArrayEquals(new byte[] {(byte) 199, 100, 50, (byte) 128}, result);
    }

    @Test
    void forcesOpaqueAlphaForOpaqueSurfaces() {
        ByteBuffer pixels = ByteBuffer.allocateDirect(4);
        pixels.put(new byte[] {1, 2, 3, 4}).flip();
        BrowserFrame frame = new BrowserFrame(1, 1, 1, List.of(new BrowserDirtyRegion(0, 0, 1, 1)), pixels);

        ByteBuffer converted =
                new GrapheneFramePixelConverter().convert(frame, new BrowserDirtyRegion(0, 0, 1, 1), false);

        byte[] result = new byte[converted.remaining()];
        converted.get(result);
        assertArrayEquals(new byte[] {3, 2, 1, (byte) 0xFF}, result);
    }
}
