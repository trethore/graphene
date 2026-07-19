package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrapheneSurfaceSizingStateTest {
  @Test
  void calculatesAndUpdatesAutomaticResolution() {
    GrapheneSurfaceSizingState state = new GrapheneSurfaceSizingState(100, 50, true, 1, 1, 1.5);

    assertEquals(150, state.resolutionWidth());
    assertEquals(75, state.resolutionHeight());

    GrapheneSurfaceSizingState.Resize resize = state.resize(200, 80, 2.0);

    assertTrue(resize.required());
    assertEquals(400, resize.width());
    assertEquals(160, resize.height());
  }

  @Test
  void preservesManualResolutionWhenLogicalSizeChanges() {
    GrapheneSurfaceSizingState state =
        new GrapheneSurfaceSizingState(100, 50, false, 320, 180, 1.0);

    GrapheneSurfaceSizingState.Resize resize = state.resize(200, 100, 2.0);

    assertFalse(resize.required());
    assertEquals(320, state.resolutionWidth());
    assertEquals(180, state.resolutionHeight());
  }

  @Test
  void switchesBetweenManualAndAutomaticResolution() {
    GrapheneSurfaceSizingState state = new GrapheneSurfaceSizingState(100, 50, true, 1, 1, 1.0);

    state.setResolution(640, 360);
    assertFalse(state.autoResolution());
    assertEquals(640, state.resolutionWidth());

    state.useAutoResolution(2.0);
    assertTrue(state.autoResolution());
    assertEquals(200, state.resolutionWidth());
    assertEquals(100, state.resolutionHeight());
  }

  @Test
  void mapsAndClampsViewportCoordinates() {
    GrapheneSurfaceSizingState state =
        new GrapheneSurfaceSizingState(100, 50, false, 200, 100, 1.0);

    assertEquals(100, state.mapX(50.0, 100));
    assertEquals(0, state.mapX(-10.0, 100));
    assertEquals(199, state.mapX(100.0, 100));
    assertEquals(99, state.mapY(500.0, 50));
  }

  @Test
  void rejectsInvalidDimensionsAndScaleFactors() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GrapheneSurfaceSizingState(0, 10, true, 1, 1, 1.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GrapheneSurfaceSizingState(10, 10, true, 1, 1, Double.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> GrapheneSurfaceSizingState.mapCoordinate(1.0, 0, 10));
  }
}
