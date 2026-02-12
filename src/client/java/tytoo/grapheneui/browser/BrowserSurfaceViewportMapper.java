package tytoo.grapheneui.browser;

final class BrowserSurfaceViewportMapper {
    private BrowserSurfaceViewportMapper() {
    }

    static int mapCoordinate(double coordinate, int renderedSize, int sourceStart, int sourceSize) {
        if (renderedSize <= 0 || sourceSize <= 0) {
            return sourceStart;
        }

        double scaledCoordinate = coordinate * sourceSize / renderedSize;
        return sourceStart + (int) scaledCoordinate;
    }
}
