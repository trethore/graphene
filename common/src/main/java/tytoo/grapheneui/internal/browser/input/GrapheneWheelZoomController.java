package tytoo.grapheneui.internal.browser.input;

public final class GrapheneWheelZoomController {
    private static final int WHEEL_DELTA_PER_STEP = 120;
    private static final double ZOOM_LEVEL_STEP = 0.2D;
    private static final double MIN_ZOOM_LEVEL = -10.0D;
    private static final double MAX_ZOOM_LEVEL = 10.0D;

    public double applyZoomDelta(double currentZoomLevel, int wheelDelta) {
        int stepCount = Math.max(1, Math.abs(wheelDelta) / WHEEL_DELTA_PER_STEP);
        double direction = Math.signum(wheelDelta);
        return Math.clamp(currentZoomLevel + direction * ZOOM_LEVEL_STEP * stepCount, MIN_ZOOM_LEVEL, MAX_ZOOM_LEVEL);
    }
}
