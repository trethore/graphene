package tytoo.grapheneui.internal.browser.render;

import java.awt.*;
import java.nio.ByteBuffer;

public final class GraphenePaintFrameView implements GraphenePaintUploadView {
    private final ByteBuffer buffer;
    private final int width;
    private final int height;
    private final Rectangle[] dirtyRects;
    private final boolean fullReRender;
    private final long frameVersion;

    GraphenePaintFrameView(
            ByteBuffer buffer,
            int width,
            int height,
            Rectangle[] dirtyRects,
            boolean fullReRender,
            long frameVersion
    ) {
        this.buffer = buffer;
        this.width = width;
        this.height = height;
        this.dirtyRects = dirtyRects;
        this.fullReRender = fullReRender;
        this.frameVersion = frameVersion;
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
}
