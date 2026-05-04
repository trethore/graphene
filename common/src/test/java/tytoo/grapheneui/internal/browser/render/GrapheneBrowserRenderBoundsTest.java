package tytoo.grapheneui.internal.browser.render;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class GrapheneBrowserRenderBoundsTest {
    @Test
    void intersectReturnsVisibleRectangle() {
        Rectangle intersection = GrapheneBrowserRenderBounds.intersect(10, 20, 50, 40, 30, 10, 50, 40);

        assertEquals(new Rectangle(30, 20, 30, 30), intersection);
    }

    @Test
    void intersectReturnsNullWhenRectanglesDoNotOverlap() {
        Rectangle intersection = GrapheneBrowserRenderBounds.intersect(0, 0, 10, 10, 10, 0, 10, 10);

        assertNull(intersection);
    }

    @Test
    void clampRegionClipsAgainstMaximumBounds() {
        GrapheneBrowserRenderBounds.Region region = GrapheneBrowserRenderBounds.clampRegion(-5, 10, 20, 20, 30, 25);

        assertEquals(new GrapheneBrowserRenderBounds.Region(0, 10, 15, 15), region);
    }

    @Test
    void scalePixelHandlesInvalidInputsAndClampsToTargetSize() {
        assertEquals(0, GrapheneBrowserRenderBounds.scalePixel(10, 0, 100));
        assertEquals(0, GrapheneBrowserRenderBounds.scalePixel(10, 100, 0));
        assertEquals(100, GrapheneBrowserRenderBounds.scalePixel(150, 100, 100));
    }

    @Test
    void placePopupMapsVisiblePopupToDestinationAndSourceRects() {
        Rectangle popupRect = new Rectangle(50, 40, 80, 40);
        GrapheneBrowserRenderBounds.Region visibleRegion = new GrapheneBrowserRenderBounds.Region(10, 20, 100, 50);

        GrapheneBrowserRenderBounds.PopupPlacement placement = GrapheneBrowserRenderBounds.placePopup(
                popupRect,
                visibleRegion,
                0,
                0,
                200,
                100,
                80,
                40
        );

        assertEquals(new GrapheneBrowserRenderBounds.PopupPlacement(80, 40, 120, 60, 0, 0, 60, 30), placement);
    }

    @Test
    void placePopupReturnsNullWhenPopupIsFullyClipped() {
        Rectangle popupRect = new Rectangle(200, 200, 20, 20);
        GrapheneBrowserRenderBounds.Region visibleRegion = new GrapheneBrowserRenderBounds.Region(0, 0, 100, 100);

        GrapheneBrowserRenderBounds.PopupPlacement placement = GrapheneBrowserRenderBounds.placePopup(
                popupRect,
                visibleRegion,
                0,
                0,
                100,
                100,
                20,
                20
        );

        assertNull(placement);
    }
}
