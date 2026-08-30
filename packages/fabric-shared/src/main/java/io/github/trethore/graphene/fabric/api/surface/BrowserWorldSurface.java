package io.github.trethore.graphene.fabric.api.surface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.minecraft.internal.render.GrapheneWorldSurfaceGeometry;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;

/**
 * Projects a browser view onto a centered rectangular plane in local XY space. The front of the
 * surface faces local positive Z and browser coordinate {@code (0, 0)} is its top-left corner.
 */
@SuppressWarnings("unused")
public final class BrowserWorldSurface {
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final BrowserView view;
    private float width;
    private float height;
    private boolean doubleSided;
    private boolean fullBright;
    private WorldSurfaceTransparency transparencyMode;
    private float opacity;

    private BrowserWorldSurface(Builder builder) {
        view = builder.view;
        width = builder.width;
        height = builder.height;
        doubleSided = builder.doubleSided;
        fullBright = builder.fullBright;
        transparencyMode = builder.transparencyMode;
        opacity = builder.opacity;
    }

    public static Builder builder(BrowserView view) {
        return new Builder(view);
    }

    public BrowserView view() {
        return view;
    }

    public BrowserSession browser() {
        return view.browser();
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public boolean isDoubleSided() {
        return doubleSided;
    }

    /** Returns whether directional surface shading is disabled. */
    public boolean isFullBright() {
        return fullBright;
    }

    /** Returns how the browser frame's alpha channel is rendered. */
    public WorldSurfaceTransparency transparencyMode() {
        return transparencyMode;
    }

    /** Returns the opacity multiplier from {@code 0.0} through {@code 1.0}. */
    public float opacity() {
        return opacity;
    }

    public void setDimensions(float width, float height) {
        this.width = requirePositive(width, "width");
        this.height = requirePositive(height, "height");
    }

    public void setDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
    }

    /** Sets whether to use maximum light and disable directional surface shading. */
    public void setFullBright(boolean fullBright) {
        this.fullBright = fullBright;
    }

    /** Sets how the browser frame's alpha channel is rendered. */
    public void setTransparencyMode(WorldSurfaceTransparency transparencyMode) {
        this.transparencyMode = Objects.requireNonNull(transparencyMode, "transparencyMode");
    }

    /**
     * Sets the surface opacity multiplier. Values below {@code 1.0} require blended rendering and
     * therefore do not write depth.
     */
    public void setOpacity(float opacity) {
        this.opacity = requireOpacity(opacity);
    }

    /**
     * Uploads the latest browser frame and submits this surface using the current pose-stack
     * transform.
     */
    public void submit(SubmitNodeCollector collector, PoseStack poseStack) {
        SubmitNodeCollector validatedCollector = Objects.requireNonNull(collector, "collector");
        PoseStack validatedPoseStack = Objects.requireNonNull(poseStack, "poseStack");
        view.prepareTexture().ifPresent(texture -> {
            RenderType renderType = renderType(texture.identifier(), transparencyMode, opacity);
            float submittedWidth = width;
            float submittedHeight = height;
            boolean submittedDoubleSided = doubleSided;
            boolean submittedFullBright = fullBright;
            int submittedAlpha = Math.round(opacity * 255.0F);
            validatedCollector.submitCustomGeometry(
                    validatedPoseStack,
                    renderType,
                    (pose, consumer) -> renderPlane(
                            pose,
                            consumer,
                            submittedWidth,
                            submittedHeight,
                            submittedDoubleSided,
                            submittedFullBright,
                            submittedAlpha));
        });
    }

    /** Intersects a world-space ray with this surface transformed by {@code localToWorld}. */
    public Optional<BrowserWorldSurfaceHit> hitTest(
            Vector3dc rayOrigin, Vector3dc rayDirection, Matrix4fc localToWorld) {
        return GrapheneWorldSurfaceGeometry.hitTest(rayOrigin, rayDirection, localToWorld, width, height, doubleSided)
                .map(hit -> new BrowserWorldSurfaceHit(
                        hit.u(),
                        hit.v(),
                        view.toBrowserX(hit.u()),
                        view.toBrowserY(hit.v()),
                        hit.worldX(),
                        hit.worldY(),
                        hit.worldZ(),
                        hit.distance()));
    }

    private static void renderPlane(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float width,
            float height,
            boolean doubleSided,
            boolean fullBright,
            int alpha) {
        float halfWidth = width / 2.0F;
        float halfHeight = height / 2.0F;
        vertex(consumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, 1.0F, fullBright, alpha);
        vertex(consumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, 1.0F, fullBright, alpha);
        vertex(consumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, 1.0F, fullBright, alpha);
        vertex(consumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, 1.0F, fullBright, alpha);
        if (doubleSided) {
            vertex(consumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, -1.0F, fullBright, alpha);
            vertex(consumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, -1.0F, fullBright, alpha);
            vertex(consumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, -1.0F, fullBright, alpha);
            vertex(consumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, -1.0F, fullBright, alpha);
        }
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalZ,
            boolean fullBright,
            int alpha) {
        VertexConsumer vertex = consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT);
        if (fullBright) {
            vertex.setNormal(0.2F, 1.0F, -0.7F);
        } else {
            vertex.setNormal(pose, 0.0F, 0.0F, normalZ);
        }
    }

    public static final class Builder {
        private final BrowserView view;
        private float width = 1.0F;
        private float height = 1.0F;
        private boolean doubleSided;
        private boolean fullBright;
        private WorldSurfaceTransparency transparencyMode = WorldSurfaceTransparency.OPAQUE;
        private float opacity = 1.0F;

        private Builder(BrowserView view) {
            this.view = Objects.requireNonNull(view, "view");
        }

        public Builder dimensions(float width, float height) {
            this.width = requirePositive(width, "width");
            this.height = requirePositive(height, "height");
            return this;
        }

        public Builder doubleSided(boolean doubleSided) {
            this.doubleSided = doubleSided;
            return this;
        }

        /** Sets whether to use maximum light and disable directional surface shading. */
        public Builder fullBright(boolean fullBright) {
            this.fullBright = fullBright;
            return this;
        }

        /** Sets how the browser frame's alpha channel is rendered. */
        public Builder transparencyMode(WorldSurfaceTransparency transparencyMode) {
            this.transparencyMode = Objects.requireNonNull(transparencyMode, "transparencyMode");
            return this;
        }

        /**
         * Sets the surface opacity multiplier. Values below {@code 1.0} require blended rendering
         * and therefore do not write depth.
         */
        public Builder opacity(float opacity) {
            this.opacity = requireOpacity(opacity);
            return this;
        }

        public BrowserWorldSurface build() {
            return new BrowserWorldSurface(this);
        }
    }

    private static float requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
        return value;
    }

    private static float requireOpacity(float opacity) {
        if (!Float.isFinite(opacity) || opacity < 0.0F || opacity > 1.0F) {
            throw new IllegalArgumentException("opacity must be finite and between 0.0 and 1.0");
        }
        return opacity;
    }

    private static RenderType renderType(Identifier texture, WorldSurfaceTransparency transparencyMode, float opacity) {
        if (opacity < 1.0F || transparencyMode == WorldSurfaceTransparency.BLENDED) {
            return RenderTypes.entityTranslucentEmissive(texture);
        }
        if (transparencyMode == WorldSurfaceTransparency.CUTOUT) {
            return RenderTypes.entityCutout(texture);
        }
        return RenderTypes.entitySolid(texture);
    }
}
