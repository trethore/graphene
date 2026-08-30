package io.github.trethore.graphene.fabric.api.surface;

/** Controls how a browser world surface uses the browser frame's alpha channel. */
public enum WorldSurfaceTransparency {
    /** Ignores texture alpha for blending and writes depth. */
    OPAQUE,

    /** Discards low-alpha pixels and writes depth for the remaining pixels. */
    CUTOUT,

    /** Preserves partial alpha through blending without writing depth. */
    BLENDED
}
