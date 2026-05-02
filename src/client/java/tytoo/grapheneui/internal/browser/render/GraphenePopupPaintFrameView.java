package tytoo.grapheneui.internal.browser.render;

import java.awt.*;
import java.nio.ByteBuffer;

public final class GraphenePopupPaintFrameView implements GraphenePaintUploadView {
    private final ByteBuffer buffer;
    private final int width;
    private final int height;
    private final Rectangle[] dirtyRects;
    private final boolean fullReRender;
    private final long frameVersion;
    private final Rectangle popupRect;

    GraphenePopupPaintFrameView(
            ByteBuffer buffer,
            int width,
            int height,
            Rectangle[] dirtyRects,
            boolean fullReRender,
            long frameVersion,
            Rectangle popupRect
    ) {
        this.buffer = buffer;
        this.width = width;
        this.height = height;
        this.dirtyRects = dirtyRects;
        this.fullReRender = fullReRender;
        this.frameVersion = frameVersion;
        this.popupRect = popupRect;
    }

    @Override
    public ByteBuffer buffer() {
        return buffer;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public Rectangle[] dirtyRects() {
        return dirtyRects;
    }

    @Override
    public boolean fullReRender() {
        return fullReRender;
    }

    @Override
    public long frameVersion() {
        return frameVersion;
    }

    public Rectangle popupRect() {
        return popupRect;
    }
}
