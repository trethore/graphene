package tytoo.grapheneui.internal.browser.render;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphenePaintBufferTest {
    @Test
    void initialSnapshotHasNoFrames() {
        GraphenePaintSnapshot snapshot = new GraphenePaintBuffer().snapshot();

        assertNull(snapshot.mainFrame());
        assertNull(snapshot.popupFrame());
    }

    @Test
    void mainFrameCaptureExposesFrameMetadataAndCopiedPixels() {
        GraphenePaintBuffer paintBuffer = new GraphenePaintBuffer();
        Rectangle[] dirtyRects = {new Rectangle(0, 0, 1, 1)};
        ByteBuffer pixels = pixels(2, 1, 2, 3, 4, 5, 6, 7, 8);

        paintBuffer.capture(false, dirtyRects, pixels, 2, 1);
        GraphenePaintFrameView frame = paintBuffer.snapshot().mainFrame();

        assertNotNull(frame);
        assertEquals(2, frame.width());
        assertEquals(1, frame.height());
        assertEquals(1, frame.frameVersion());
        assertFalse(frame.fullReRender());
        assertEquals(new Rectangle(0, 0, 1, 1), frame.dirtyRects()[0]);
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, readBytes(frame.buffer(), 8));
    }

    @Test
    void dirtyRectsAreCopiedWhenCaptured() {
        GraphenePaintBuffer paintBuffer = new GraphenePaintBuffer();
        Rectangle dirtyRect = new Rectangle(0, 0, 1, 1);
        Rectangle[] dirtyRects = {dirtyRect};

        paintBuffer.capture(false, dirtyRects, pixels(1, 1, 2, 3, 4), 1, 1);
        dirtyRect.setBounds(10, 10, 20, 20);
        dirtyRects[0] = new Rectangle(30, 30, 40, 40);

        assertEquals(new Rectangle(0, 0, 1, 1), paintBuffer.snapshot().mainFrame().dirtyRects()[0]);
    }

    @Test
    void resizedMainFrameForcesFullRerender() {
        GraphenePaintBuffer paintBuffer = new GraphenePaintBuffer();

        paintBuffer.capture(false, null, pixels(1, 1, 2, 3, 4), 1, 1);
        paintBuffer.capture(false, null, pixels(2, 1, 2, 3, 4, 5, 6, 7, 8), 2, 1);

        assertTrue(paintBuffer.snapshot().mainFrame().fullReRender());
    }

    @Test
    void popupFrameAppearsOnlyWhenPopupIsVisibleAndCaptured() {
        GraphenePaintBuffer paintBuffer = new GraphenePaintBuffer();

        paintBuffer.capture(true, null, pixels(1, 1, 2, 3, 4), 1, 1);
        assertNull(paintBuffer.snapshot().popupFrame());

        paintBuffer.onPopupSize(new Rectangle(10, 20, 30, 40));
        paintBuffer.capture(true, null, pixels(1, 5, 6, 7, 8), 1, 1);
        GraphenePopupPaintFrameView popupFrame = paintBuffer.snapshot().popupFrame();

        assertNotNull(popupFrame);
        assertEquals(1, popupFrame.width());
        assertEquals(1, popupFrame.height());
        assertEquals(new Rectangle(10, 20, 30, 40), popupFrame.popupRect());
        assertArrayEquals(new byte[]{5, 6, 7, 8}, readBytes(popupFrame.buffer(), 4));
    }

    @Test
    void popupCloseRemovesPopupFrameFromSnapshot() {
        GraphenePaintBuffer paintBuffer = new GraphenePaintBuffer();

        paintBuffer.onPopupSize(new Rectangle(10, 20, 30, 40));
        paintBuffer.capture(true, null, pixels(1, 1, 2, 3, 4), 1, 1);
        paintBuffer.onPopupClosed();

        assertNull(paintBuffer.snapshot().popupFrame());
    }

    private static ByteBuffer pixels(int width, int... values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect((width) << 2);
        for (int value : values) {
            buffer.put((byte) value);
        }
        buffer.position(0);
        return buffer;
    }

    private static byte[] readBytes(ByteBuffer buffer, int length) {
        ByteBuffer copy = buffer.duplicate();
        copy.position(0);
        byte[] bytes = new byte[length];
        copy.get(bytes);
        return bytes;
    }
}
