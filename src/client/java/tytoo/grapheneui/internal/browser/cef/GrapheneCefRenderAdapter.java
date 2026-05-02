package tytoo.grapheneui.internal.browser.cef;

import com.mojang.blaze3d.platform.cursor.CursorType;
import org.cef.handler.CefScreenInfo;
import tytoo.grapheneui.internal.browser.input.GrapheneCursorMapper;
import tytoo.grapheneui.internal.browser.render.GraphenePaintBuffer;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class GrapheneCefRenderAdapter {
    private final GraphenePaintBuffer paintBuffer;
    private final Runnable invalidator;
    private final Rectangle browserRect = new Rectangle(0, 0, 1, 1);
    private final Point screenPoint = new Point(0, 0);
    private volatile int cursorType = Cursor.DEFAULT_CURSOR;

    public GrapheneCefRenderAdapter(GraphenePaintBuffer paintBuffer, Runnable invalidator) {
        this.paintBuffer = Objects.requireNonNull(paintBuffer, "paintBuffer");
        this.invalidator = Objects.requireNonNull(invalidator, "invalidator");
    }

    public Rectangle getViewRect() {
        return browserRect;
    }

    public boolean getScreenInfo(CefScreenInfo screenInfo) {
        screenInfo.Set(1.0, 32, 8, false, browserRect.getBounds(), browserRect.getBounds());
        return true;
    }

    public Point getScreenPoint(Point viewPoint) {
        Point point = new Point(screenPoint);
        point.translate(viewPoint.x, viewPoint.y);
        return point;
    }

    public void onPopupShow(boolean show) {
        if (show) {
            return;
        }

        paintBuffer.onPopupClosed();
        invalidator.run();
    }

    public void onPopupSize(Rectangle size) {
        paintBuffer.onPopupSize(size);
    }

    public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        paintBuffer.capture(popup, dirtyRects, buffer, width, height);
    }

    public boolean onCursorChange(int cursorType) {
        this.cursorType = cursorType;
        return true;
    }

    public void resize(int width, int height) {
        browserRect.setBounds(0, 0, width, height);
    }

    public CursorType requestedCursor() {
        return GrapheneCursorMapper.toMinecraftCursor(cursorType);
    }
}
