package io.github.trethore.graphene.minecraft.internal.render;

import java.util.Objects;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;

public final class GrapheneWorldSurfaceGeometry {
    private GrapheneWorldSurfaceGeometry() {}

    public static Optional<Hit> hitTest(
            Vector3dc rayOrigin,
            Vector3dc rayDirection,
            Matrix4fc localToWorld,
            double width,
            double height,
            boolean doubleSided) {
        Vector3dc validatedOrigin = Objects.requireNonNull(rayOrigin, "rayOrigin");
        Vector3dc validatedDirection = Objects.requireNonNull(rayDirection, "rayDirection");
        Matrix4fc validatedTransform = Objects.requireNonNull(localToWorld, "localToWorld");
        if (validatedDirection.lengthSquared() == 0.0) {
            throw new IllegalArgumentException("rayDirection must not be zero");
        }

        Matrix4f worldToLocal = new Matrix4f(validatedTransform);
        if (!Float.isFinite(worldToLocal.determinant()) || Math.abs(worldToLocal.determinant()) < 1.0E-9F) {
            throw new IllegalArgumentException("localToWorld must be invertible");
        }
        worldToLocal.invert();
        Vector3d localOrigin = transformPosition(worldToLocal, validatedOrigin);
        Vector3d localDirection = transformDirection(worldToLocal, validatedDirection);
        if (Math.abs(localDirection.z) < 1.0E-9) {
            return Optional.empty();
        }
        if (!doubleSided && localDirection.z >= 0.0) {
            return Optional.empty();
        }
        double parameter = -localOrigin.z / localDirection.z;
        if (parameter < 0.0) {
            return Optional.empty();
        }

        double localX = localOrigin.x + localDirection.x * parameter;
        double localY = localOrigin.y + localDirection.y * parameter;
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        if (localX < -halfWidth || localX > halfWidth || localY < -halfHeight || localY > halfHeight) {
            return Optional.empty();
        }

        double u = localX / width + 0.5;
        double v = 0.5 - localY / height;
        Vector3d worldHit = transformPosition(validatedTransform, new Vector3d(localX, localY, 0.0));
        return Optional.of(new Hit(u, v, worldHit.x, worldHit.y, worldHit.z, worldHit.distance(validatedOrigin)));
    }

    private static Vector3d transformPosition(Matrix4fc matrix, Vector3dc position) {
        Vector4d transformed = new Vector4d(position.x(), position.y(), position.z(), 1.0).mul(matrix);
        if (transformed.w != 0.0 && transformed.w != 1.0) {
            transformed.div(transformed.w);
        }
        return new Vector3d(transformed.x, transformed.y, transformed.z);
    }

    private static Vector3d transformDirection(Matrix4fc matrix, Vector3dc direction) {
        Vector4d transformed = new Vector4d(direction.x(), direction.y(), direction.z(), 0.0).mul(matrix);
        return new Vector3d(transformed.x, transformed.y, transformed.z);
    }

    public record Hit(double u, double v, double worldX, double worldY, double worldZ, double distance) {}
}
