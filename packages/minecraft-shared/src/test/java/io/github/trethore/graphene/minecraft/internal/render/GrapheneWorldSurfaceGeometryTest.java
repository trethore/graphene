package io.github.trethore.graphene.minecraft.internal.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

final class GrapheneWorldSurfaceGeometryTest {
    private static final double DELTA = 1.0E-6;

    @Test
    void intersectsCenterFromFront() {
        Optional<GrapheneWorldSurfaceGeometry.Hit> result = GrapheneWorldSurfaceGeometry.hitTest(
                new Vector3d(0.0, 0.0, 2.0), new Vector3d(0.0, 0.0, -1.0), new Matrix4f(), 2.0, 1.0, false);

        assertTrue(result.isPresent());
        GrapheneWorldSurfaceGeometry.Hit hit = result.orElseThrow();
        assertEquals(0.5, hit.u(), DELTA);
        assertEquals(0.5, hit.v(), DELTA);
        assertEquals(2.0, hit.distance(), DELTA);
    }

    @Test
    void mapsTopRightToUv() {
        GrapheneWorldSurfaceGeometry.Hit hit = GrapheneWorldSurfaceGeometry.hitTest(
                        new Vector3d(1.0, 0.5, 1.0), new Vector3d(0.0, 0.0, -1.0), new Matrix4f(), 2.0, 1.0, false)
                .orElseThrow();

        assertEquals(1.0, hit.u(), DELTA);
        assertEquals(0.0, hit.v(), DELTA);
    }

    @Test
    void appliesWorldTransform() {
        Matrix4f transform = new Matrix4f().translation(4.0F, 5.0F, 6.0F);

        GrapheneWorldSurfaceGeometry.Hit hit = GrapheneWorldSurfaceGeometry.hitTest(
                        new Vector3d(4.0, 5.0, 8.0), new Vector3d(0.0, 0.0, -1.0), transform, 2.0, 1.0, false)
                .orElseThrow();

        assertEquals(4.0, hit.worldX(), DELTA);
        assertEquals(5.0, hit.worldY(), DELTA);
        assertEquals(6.0, hit.worldZ(), DELTA);
    }

    @Test
    void rejectsBackFaceUnlessDoubleSided() {
        Vector3d origin = new Vector3d(0.0, 0.0, -1.0);
        Vector3d direction = new Vector3d(0.0, 0.0, 1.0);

        assertTrue(GrapheneWorldSurfaceGeometry.hitTest(origin, direction, new Matrix4f(), 1.0, 1.0, false)
                .isEmpty());
        assertTrue(GrapheneWorldSurfaceGeometry.hitTest(origin, direction, new Matrix4f(), 1.0, 1.0, true)
                .isPresent());
    }

    @Test
    void rejectsSingularTransform() {
        Matrix4f transform = new Matrix4f().scale(0.0F);
        Vector3d origin = new Vector3d();
        Vector3d direction = new Vector3d(0.0, 0.0, -1.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> GrapheneWorldSurfaceGeometry.hitTest(origin, direction, transform, 1.0, 1.0, false));
    }
}
