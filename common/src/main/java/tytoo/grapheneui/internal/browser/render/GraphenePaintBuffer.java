package tytoo.grapheneui.internal.browser.render;

import java.awt.*;
import java.nio.ByteBuffer;

public final class GraphenePaintBuffer {
    private static final int SLOT_COUNT = 4;

    private final MainFrameData mainFrameData = new MainFrameData();
    private final PopupFrameData popupFrameData = new PopupFrameData();

    private static void copyBuffer(ByteBuffer sourceBuffer, ByteBuffer targetBuffer, int width, int height) {
        int size = (width * height) << 2;
        ByteBuffer sourceCopy = sourceBuffer.duplicate();
        sourceCopy.position(0);
        sourceCopy.limit(size);

        targetBuffer.position(0);
        targetBuffer.limit(size);
        targetBuffer.put(sourceCopy);
        targetBuffer.position(0);
    }

    private static Rectangle[] copyDirtyRects(Rectangle[] dirtyRects) {
        if (dirtyRects == null || dirtyRects.length == 0) {
            return dirtyRects;
        }

        Rectangle[] copy = new Rectangle[dirtyRects.length];
        for (int index = 0; index < dirtyRects.length; index++) {
            Rectangle rect = dirtyRects[index];
            copy[index] = rect == null ? null : new Rectangle(rect);
        }
        return copy;
    }

    public void capture(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (popup) {
            capturePopup(buffer, width, height);
            return;
        }

        captureMain(dirtyRects, buffer, width, height);
    }

    public void onPopupSize(Rectangle rect) {
        synchronized (popupFrameData) {
            if (rect.width <= 0 || rect.height <= 0) {
                popupFrameData.popupVisible = false;
                popupFrameData.popupRect.setBounds(0, 0, 0, 0);
                return;
            }

            popupFrameData.popupRect.setBounds(rect);
            popupFrameData.popupVisible = true;
        }
    }

    public void onPopupClosed() {
        synchronized (popupFrameData) {
            popupFrameData.popupVisible = false;
            popupFrameData.popupRect.setBounds(0, 0, 0, 0);
        }
    }

    public GraphenePaintSnapshot snapshot() {
        synchronized (mainFrameData) {
            synchronized (popupFrameData) {
                return new GraphenePaintSnapshot(createMainFrameView(), createPopupFrameView());
            }
        }
    }

    private void captureMain(Rectangle[] dirtyRects, ByteBuffer sourceBuffer, int width, int height) {
        synchronized (mainFrameData) {
            int writeIndex = mainFrameData.selectWriteSlot();
            FrameSlot slot = mainFrameData.ensureSlot(writeIndex, width, height);
            copyBuffer(sourceBuffer, slot.buffer, width, height);

            boolean forceFullReRender = shouldForceMainFrameRerender(writeIndex, width, height);
            slot.width = width;
            slot.height = height;
            slot.dirtyRects = copyDirtyRects(dirtyRects);
            slot.fullReRender = forceFullReRender;
            slot.frameVersion = ++mainFrameData.frameVersion;

            mainFrameData.latestSlotIndex = writeIndex;
            mainFrameData.nextSlotIndex = (writeIndex + 1) % SLOT_COUNT;
        }
    }

    private boolean shouldForceMainFrameRerender(int writeIndex, int width, int height) {
        boolean forceFullReRender = mainFrameData.latestSlotIndex == writeIndex;
        if (mainFrameData.latestSlotIndex < 0) {
            return forceFullReRender;
        }

        FrameSlot previousSlot = mainFrameData.slots[mainFrameData.latestSlotIndex];
        return forceFullReRender || previousSlot != null && (previousSlot.width != width || previousSlot.height != height);
    }

    private void capturePopup(ByteBuffer sourceBuffer, int width, int height) {
        synchronized (popupFrameData) {
            int writeIndex = popupFrameData.selectWriteSlot();
            FrameSlot slot = popupFrameData.ensureSlot(writeIndex, width, height);
            copyBuffer(sourceBuffer, slot.buffer, width, height);

            slot.width = width;
            slot.height = height;
            slot.dirtyRects = null;
            slot.fullReRender = false;
            slot.frameVersion = ++popupFrameData.frameVersion;

            popupFrameData.latestSlotIndex = writeIndex;
            popupFrameData.nextSlotIndex = (writeIndex + 1) % SLOT_COUNT;
        }
    }

    private GraphenePaintFrameView createMainFrameView() {
        FrameSlot latestSlot = mainFrameData.latestSlot();
        if (latestSlot == null) {
            return null;
        }

        mainFrameData.protectedSlot = mainFrameData.latestSlotIndex;
        return new GraphenePaintFrameView(
                latestSlot.buffer,
                latestSlot.width,
                latestSlot.height,
                latestSlot.dirtyRects,
                latestSlot.fullReRender,
                latestSlot.frameVersion
        );
    }

    private GraphenePopupPaintFrameView createPopupFrameView() {
        if (!popupFrameData.popupVisible) {
            return null;
        }

        FrameSlot latestSlot = popupFrameData.latestSlot();
        if (latestSlot == null) {
            return null;
        }

        popupFrameData.protectedSlot = popupFrameData.latestSlotIndex;
        return new GraphenePopupPaintFrameView(
                latestSlot.buffer,
                latestSlot.width,
                latestSlot.height,
                latestSlot.dirtyRects,
                latestSlot.fullReRender,
                latestSlot.frameVersion,
                new Rectangle(popupFrameData.popupRect)
        );
    }

    private static class FrameData {
        final FrameSlot[] slots = new FrameSlot[SLOT_COUNT];
        int nextSlotIndex;
        int latestSlotIndex = -1;
        int protectedSlot = -1;
        long frameVersion;

        int selectWriteSlot() {
            for (int offset = 0; offset < SLOT_COUNT; offset++) {
                int index = (nextSlotIndex + offset) % SLOT_COUNT;
                if (index != protectedSlot) {
                    return index;
                }
            }

            return nextSlotIndex;
        }

        FrameSlot ensureSlot(int slotIndex, int width, int height) {
            FrameSlot slot = slots[slotIndex];
            if (slot == null) {
                slot = new FrameSlot();
                slots[slotIndex] = slot;
            }

            int size = (width * height) << 2;
            if (slot.buffer == null || slot.buffer.capacity() != size) {
                slot.buffer = ByteBuffer.allocateDirect(size);
            }

            return slot;
        }

        FrameSlot latestSlot() {
            if (latestSlotIndex < 0) {
                return null;
            }

            return slots[latestSlotIndex];
        }
    }

    private static final class MainFrameData extends FrameData {
    }

    private static final class PopupFrameData extends FrameData {
        final Rectangle popupRect = new Rectangle();
        boolean popupVisible;
    }

    private static final class FrameSlot {
        private ByteBuffer buffer;
        private int width;
        private int height;
        private Rectangle[] dirtyRects;
        private boolean fullReRender;
        private long frameVersion;
    }
}
