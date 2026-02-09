package tytoo.grapheneui.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BrowserSurfaceViewportMapperTest {
    @Test
    void mapCoordinateReturnsSourceStartWhenSizesAreInvalid() {
        assertEquals(5, BrowserSurfaceViewportMapper.mapCoordinate(10.0, 0, 5, 100));
        assertEquals(5, BrowserSurfaceViewportMapper.mapCoordinate(10.0, 100, 5, 0));
    }

    @Test
    void mapCoordinateScalesCoordinateIntoSourceRange() {
        int mappedCoordinate = BrowserSurfaceViewportMapper.mapCoordinate(50.0, 100, 10, 200);

        assertEquals(110, mappedCoordinate);
    }

    @Test
    void mapCoordinateUsesTruncationForFractionalValues() {
        int mappedCoordinate = BrowserSurfaceViewportMapper.mapCoordinate(33.9, 100, 10, 200);

        assertEquals(77, mappedCoordinate);
    }
}
