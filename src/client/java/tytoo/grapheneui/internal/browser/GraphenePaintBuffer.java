package tytoo.grapheneui.internal.browser;

import tytoo.grapheneui.api.render.GrapheneRenderer;

import java.awt.*;
import java.nio.ByteBuffer;

final class GraphenePaintBuffer {
    private final MainFrameData mainFrameData = new MainFrameData();
    private final PopupFrameData popupFrameData = new PopupFrameData();

    void capture(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (popup) {
            synchronized (popupFrameData) {
                int size = (width * height) << 2;
                if (popupFrameData.buffer == null || popupFrameData.buffer.capacity() != size) {
                    popupFrameData.buffer = ByteBuffer.allocateDirect(size);
                }

                buffer.position(0);
                popupFrameData.buffer.position(0);
                popupFrameData.buffer.limit(buffer.limit());
                popupFrameData.buffer.put(buffer);
                popupFrameData.buffer.position(0);

                popupFrameData.width = width;
                popupFrameData.height = height;
                popupFrameData.hasFrame = true;
            }
            return;
        }

        synchronized (mainFrameData) {
            int size = (width * height) << 2;
            if (mainFrameData.buffer == null || mainFrameData.buffer.capacity() != size) {
                mainFrameData.buffer = ByteBuffer.allocateDirect(size);
            }

            if (mainFrameData.hasFrame) {
                mainFrameData.fullReRender = true;
            }

            mainFrameData.buffer.position(0);
            mainFrameData.buffer.limit(buffer.limit());
            buffer.position(0);
            mainFrameData.buffer.put(buffer);
            mainFrameData.buffer.position(0);

            mainFrameData.width = width;
            mainFrameData.height = height;
            mainFrameData.dirtyRects = dirtyRects;
            mainFrameData.hasFrame = true;
        }
    }

    void flushTo(GrapheneRenderer renderer) {
        synchronized (mainFrameData) {
            if (mainFrameData.hasFrame) {
                renderer.onPaint(
                        false,
                        mainFrameData.dirtyRects,
                        mainFrameData.buffer,
                        mainFrameData.width,
                        mainFrameData.height,
                        mainFrameData.fullReRender
                );
                mainFrameData.hasFrame = false;
                mainFrameData.fullReRender = false;
            }
        }

        synchronized (popupFrameData) {
            if (popupFrameData.hasFrame) {
                renderer.onPaint(true, null, popupFrameData.buffer, popupFrameData.width, popupFrameData.height, false);
                popupFrameData.hasFrame = false;
            }
        }
    }

    private static final class MainFrameData {
        private ByteBuffer buffer;
        private int width;
        private int height;
        private Rectangle[] dirtyRects;
        private boolean hasFrame;
        private boolean fullReRender;
    }

    private static final class PopupFrameData {
        private ByteBuffer buffer;
        private int width;
        private int height;
        private boolean hasFrame;
    }
}
