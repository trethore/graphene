package tytoo.grapheneui.internal.browser;

import java.awt.*;

final class GrapheneBrowserRenderBounds {
    private GrapheneBrowserRenderBounds() {
    }

    static Region clampRegion(int x, int y, int width, int height, int maxWidth, int maxHeight) {
        Rectangle rectangle = intersect(x, y, width, height, 0, 0, maxWidth, maxHeight);
        return rectangle == null ? null : new Region(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    static Rectangle intersect(int x, int y, int width, int height, int clipX, int clipY, int clipWidth, int clipHeight) {
        int left = Math.max(x, clipX);
        int top = Math.max(y, clipY);
        int right = Math.min(x + width, clipX + clipWidth);
        int bottom = Math.min(y + height, clipY + clipHeight);
        int visibleWidth = right - left;
        int visibleHeight = bottom - top;
        if (visibleWidth <= 0 || visibleHeight <= 0) {
            return null;
        }

        return new Rectangle(left, top, visibleWidth, visibleHeight);
    }

    static int scalePixel(int value, int targetSize, int referenceSize) {
        if (value <= 0 || targetSize <= 0 || referenceSize <= 0) {
            return 0;
        }

        return (int) Math.min(targetSize, (long) value * targetSize / referenceSize);
    }

    static PopupPlacement placePopup(
            Rectangle popupRect,
            Region visibleRegion,
            int x,
            int y,
            int width,
            int height,
            int popupFrameWidth,
            int popupFrameHeight
    ) {
        Rectangle visiblePopupRect = intersect(
                popupRect.x,
                popupRect.y,
                popupRect.width,
                popupRect.height,
                visibleRegion.x(),
                visibleRegion.y(),
                visibleRegion.width(),
                visibleRegion.height()
        );
        if (visiblePopupRect == null) {
            return null;
        }

        int destinationX = x + scalePixel(visiblePopupRect.x - visibleRegion.x(), width, visibleRegion.width());
        int destinationY = y + scalePixel(visiblePopupRect.y - visibleRegion.y(), height, visibleRegion.height());
        int destinationRight = x + scalePixel(
                visiblePopupRect.x - visibleRegion.x() + visiblePopupRect.width,
                width,
                visibleRegion.width()
        );
        int destinationBottom = y + scalePixel(
                visiblePopupRect.y - visibleRegion.y() + visiblePopupRect.height,
                height,
                visibleRegion.height()
        );
        int destinationWidth = destinationRight - destinationX;
        int destinationHeight = destinationBottom - destinationY;
        if (destinationWidth <= 0 || destinationHeight <= 0) {
            return null;
        }

        int sourceX = scalePixel(visiblePopupRect.x - popupRect.x, popupFrameWidth, popupRect.width);
        int sourceY = scalePixel(visiblePopupRect.y - popupRect.y, popupFrameHeight, popupRect.height);
        int sourceRight = scalePixel(
                visiblePopupRect.x - popupRect.x + visiblePopupRect.width,
                popupFrameWidth,
                popupRect.width
        );
        int sourceBottom = scalePixel(
                visiblePopupRect.y - popupRect.y + visiblePopupRect.height,
                popupFrameHeight,
                popupRect.height
        );
        int sourceWidth = sourceRight - sourceX;
        int sourceHeight = sourceBottom - sourceY;
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return null;
        }

        return new PopupPlacement(
                destinationX,
                destinationY,
                destinationWidth,
                destinationHeight,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight
        );
    }

    record Region(int x, int y, int width, int height) {
    }

    record PopupPlacement(
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
    }
}
