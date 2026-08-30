package io.github.trethore.graphene.fabric.api.surface;

/** Result of intersecting a world-space ray with a browser world surface. */
public record BrowserWorldSurfaceHit(
        double u, double v, int browserX, int browserY, double worldX, double worldY, double worldZ, double distance) {}
