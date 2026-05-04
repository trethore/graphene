package tytoo.grapheneui.internal.browser.drag;

import org.cef.callback.CefDragData;

import java.awt.*;

public final class GrapheneDragSession {
    private final Object lock = new Object();
    private final DragCallbacks callbacks;

    private CefDragData activeDragData;
    private int activeDragMask = CefDragData.DragOperations.DRAG_OPERATION_NONE;
    private boolean dragTargetEntered;

    public GrapheneDragSession(DragCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    private static int preferredOperation(int mask) {
        if ((mask & CefDragData.DragOperations.DRAG_OPERATION_MOVE) != 0) {
            return CefDragData.DragOperations.DRAG_OPERATION_MOVE;
        }

        if ((mask & CefDragData.DragOperations.DRAG_OPERATION_COPY) != 0) {
            return CefDragData.DragOperations.DRAG_OPERATION_COPY;
        }

        if ((mask & CefDragData.DragOperations.DRAG_OPERATION_LINK) != 0) {
            return CefDragData.DragOperations.DRAG_OPERATION_LINK;
        }

        return CefDragData.DragOperations.DRAG_OPERATION_NONE;
    }

    public boolean start(CefDragData dragData, int mask) {
        if (dragData == null) {
            return false;
        }

        synchronized (lock) {
            closeLocked();
            activeDragData = dragData.clone();
            activeDragMask = mask;
            dragTargetEntered = false;
        }
        return true;
    }

    public void update(int x, int y, int modifiers) {
        synchronized (lock) {
            if (activeDragData == null) {
                return;
            }

            Point point = new Point(x, y);
            if (!dragTargetEntered) {
                callbacks.enter(activeDragData, point, modifiers, activeDragMask);
                dragTargetEntered = true;
                return;
            }

            callbacks.over(point, modifiers, activeDragMask);
        }
    }

    public void complete(int x, int y, int modifiers) {
        synchronized (lock) {
            if (activeDragData == null) {
                return;
            }

            Point point = new Point(x, y);
            if (!dragTargetEntered) {
                callbacks.enter(activeDragData, point, modifiers, activeDragMask);
                dragTargetEntered = true;
            }

            callbacks.drop(point, modifiers);
            callbacks.sourceEndedAt(point, preferredOperation(activeDragMask));
            callbacks.systemDragEnded();
            clearLocked();
        }
    }

    public void cancel() {
        synchronized (lock) {
            closeLocked();
        }
    }

    private void closeLocked() {
        if (activeDragData == null) {
            return;
        }

        if (dragTargetEntered) {
            callbacks.leave();
        }

        callbacks.systemDragEnded();
        clearLocked();
    }

    private void clearLocked() {
        if (activeDragData != null) {
            activeDragData.dispose();
            activeDragData = null;
        }

        activeDragMask = CefDragData.DragOperations.DRAG_OPERATION_NONE;
        dragTargetEntered = false;
    }

    public interface DragCallbacks {
        void enter(CefDragData dragData, Point point, int modifiers, int operationMask);

        void over(Point point, int modifiers, int operationMask);

        void drop(Point point, int modifiers);

        void leave();

        void sourceEndedAt(Point point, int operation);

        void systemDragEnded();
    }
}
