package tytoo.grapheneui.internal.browser;

final class BrowserSurfaceViewportMapper {
    private BrowserSurfaceViewportMapper() {
    }

    static int mapCoordinate(double coordinate, int renderedSize, int sourceStart, int sourceSize) {
        if (renderedSize <= 0 || sourceSize <= 0) {
            return sourceStart;
        }

        double scaledCoordinate = coordinate * sourceSize / renderedSize;
        int mappedCoordinate = sourceStart + (int) scaledCoordinate;
        int maxCoordinate = sourceStart + sourceSize - 1;
        return Math.clamp(mappedCoordinate, sourceStart, maxCoordinate);
    }
}
