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
import org.joml.Matrix4fc;
import org.joml.Vector3dc;

/**
 * Projects a browser view onto a centered rectangular plane in local XY space. The front of the
 * surface faces local positive Z and browser coordinate {@code (0, 0)} is its top-left corner.
 */
@SuppressWarnings("unused")
public final class BrowserWorldSurface {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int NO_OVERLAY = 0;

    private final BrowserView view;
    private float width;
    private float height;
    private boolean doubleSided;

    private BrowserWorldSurface(Builder builder) {
        view = builder.view;
        width = builder.width;
        height = builder.height;
        doubleSided = builder.doubleSided;
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

    public void setDimensions(float width, float height) {
        this.width = requirePositive(width, "width");
        this.height = requirePositive(height, "height");
    }

    public void setDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
    }

    /**
     * Uploads the latest browser frame and submits this surface using the current pose-stack
     * transform.
     */
    public void submit(SubmitNodeCollector collector, PoseStack poseStack) {
        SubmitNodeCollector validatedCollector = Objects.requireNonNull(collector, "collector");
        PoseStack validatedPoseStack = Objects.requireNonNull(poseStack, "poseStack");
        view.prepareTexture().ifPresent(texture -> {
            RenderType renderType = view.browser().options().transparent()
                    ? RenderTypes.entityTranslucentEmissive(texture.identifier())
                    : RenderTypes.entitySolid(texture.identifier());
            float submittedWidth = width;
            float submittedHeight = height;
            boolean submittedDoubleSided = doubleSided;
            validatedCollector.submitCustomGeometry(
                    validatedPoseStack,
                    renderType,
                    (pose, consumer) ->
                            renderPlane(pose, consumer, submittedWidth, submittedHeight, submittedDoubleSided));
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
            PoseStack.Pose pose, VertexConsumer consumer, float width, float height, boolean doubleSided) {
        float halfWidth = width / 2.0F;
        float halfHeight = height / 2.0F;
        vertex(consumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, 1.0F);
        vertex(consumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, 1.0F);
        vertex(consumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, 1.0F);
        vertex(consumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, 1.0F);
        if (doubleSided) {
            vertex(consumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, -1.0F);
            vertex(consumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, -1.0F);
            vertex(consumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, -1.0F);
            vertex(consumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, -1.0F);
        }
    }

    private static void vertex(
            VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float normalZ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, normalZ);
    }

    public static final class Builder {
        private final BrowserView view;
        private float width = 1.0F;
        private float height = 1.0F;
        private boolean doubleSided;

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
}
