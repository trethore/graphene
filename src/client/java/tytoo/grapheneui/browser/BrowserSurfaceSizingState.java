package tytoo.grapheneui.browser;

import java.awt.*;

final class BrowserSurfaceSizingState {
    private static final int MIN_SIZE = 1;

    private final Rectangle viewBox = new Rectangle(0, 0, MIN_SIZE, MIN_SIZE);
    private int surfaceWidth;
    private int surfaceHeight;
    private int resolutionWidth;
    private int resolutionHeight;
    private boolean autoResolution;
    private boolean customViewBox;

    BrowserSurfaceSizingState(
            int surfaceWidth,
            int surfaceHeight,
            boolean autoResolution,
            int resolutionWidth,
            int resolutionHeight,
            Rectangle initialViewBox,
            double initialScaleX,
            double initialScaleY
    ) {
        this.surfaceWidth = requirePositive(surfaceWidth, "surfaceWidth");
        this.surfaceHeight = requirePositive(surfaceHeight, "surfaceHeight");
        this.autoResolution = autoResolution;

        if (autoResolution) {
            updateResolutionFromSurface(initialScaleX, initialScaleY);
        } else {
            setResolutionInternal(resolutionWidth, resolutionHeight);
        }

        if (initialViewBox != null) {
            customViewBox = true;
            setViewBoxInternal(initialViewBox);
        } else {
            syncViewBoxToResolution();
        }
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }

        return value;
    }

    int surfaceWidth() {
        return surfaceWidth;
    }

    int surfaceHeight() {
        return surfaceHeight;
    }

    int resolutionWidth() {
        return resolutionWidth;
    }

    int resolutionHeight() {
        return resolutionHeight;
    }

    Rectangle viewBox() {
        return new Rectangle(viewBox);
    }

    int viewBoxX() {
        return viewBox.x;
    }

    int viewBoxY() {
        return viewBox.y;
    }

    int viewBoxWidth() {
        return viewBox.width;
    }

    int viewBoxHeight() {
        return viewBox.height;
    }

    boolean isAutoResolution() {
        return autoResolution;
    }

    ResizeInstruction setSurfaceSize(int width, int height, double scaleX, double scaleY) {
        this.surfaceWidth = requirePositive(width, "surfaceWidth");
        this.surfaceHeight = requirePositive(height, "surfaceHeight");

        if (!autoResolution) {
            return ResizeInstruction.noResize();
        }

        updateResolutionFromSurface(scaleX, scaleY);
        return ResizeInstruction.resizeTo(resolutionWidth, resolutionHeight);
    }

    ResizeInstruction setResolution(int width, int height) {
        autoResolution = false;
        setResolutionInternal(width, height);
        return ResizeInstruction.resizeTo(resolutionWidth, resolutionHeight);
    }

    ResizeInstruction useAutoResolution(double scaleX, double scaleY) {
        autoResolution = true;
        updateResolutionFromSurface(scaleX, scaleY);
        return ResizeInstruction.resizeTo(resolutionWidth, resolutionHeight);
    }

    void setViewBox(int x, int y, int width, int height) {
        customViewBox = true;
        setViewBoxInternal(new Rectangle(x, y, width, height));
    }

    void resetViewBox() {
        customViewBox = false;
        syncViewBoxToResolution();
    }

    Point toBrowserPoint(double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        int browserX = toBrowserX(surfaceX, renderedWidth);
        int browserY = toBrowserY(surfaceY, renderedHeight);
        return new Point(browserX, browserY);
    }

    int toBrowserX(double surfaceX, int renderedWidth) {
        return BrowserSurfaceViewportMapper.mapCoordinate(surfaceX, renderedWidth, viewBox.x, viewBox.width);
    }

    int toBrowserY(double surfaceY, int renderedHeight) {
        return BrowserSurfaceViewportMapper.mapCoordinate(surfaceY, renderedHeight, viewBox.y, viewBox.height);
    }

    private void setResolutionInternal(int width, int height) {
        this.resolutionWidth = requirePositive(width, "resolutionWidth");
        this.resolutionHeight = requirePositive(height, "resolutionHeight");

        if (customViewBox) {
            clampViewBoxToResolution();
        } else {
            syncViewBoxToResolution();
        }
    }

    private void updateResolutionFromSurface(double scaleX, double scaleY) {
        int calculatedWidth = (int) Math.max(MIN_SIZE, Math.round(surfaceWidth * scaleX));
        int calculatedHeight = (int) Math.max(MIN_SIZE, Math.round(surfaceHeight * scaleY));
        setResolutionInternal(calculatedWidth, calculatedHeight);
    }

    private void syncViewBoxToResolution() {
        viewBox.setBounds(0, 0, resolutionWidth, resolutionHeight);
    }

    private void setViewBoxInternal(Rectangle requestedViewBox) {
        int validatedWidth = requirePositive(requestedViewBox.width, "viewBoxWidth");
        int validatedHeight = requirePositive(requestedViewBox.height, "viewBoxHeight");

        int clampedX = Math.clamp(requestedViewBox.x, 0, Math.max(0, resolutionWidth - 1));
        int clampedY = Math.clamp(requestedViewBox.y, 0, Math.max(0, resolutionHeight - 1));
        int clampedWidth = Math.clamp(validatedWidth, MIN_SIZE, resolutionWidth - clampedX);
        int clampedHeight = Math.clamp(validatedHeight, MIN_SIZE, resolutionHeight - clampedY);
        viewBox.setBounds(clampedX, clampedY, clampedWidth, clampedHeight);
    }

    private void clampViewBoxToResolution() {
        setViewBoxInternal(viewBox);
    }

    record ResizeInstruction(boolean shouldResizeBrowser, int width, int height) {
        private static ResizeInstruction noResize() {
            return new ResizeInstruction(false, MIN_SIZE, MIN_SIZE);
        }

        private static ResizeInstruction resizeTo(int width, int height) {
            return new ResizeInstruction(true, width, height);
        }
    }
}
