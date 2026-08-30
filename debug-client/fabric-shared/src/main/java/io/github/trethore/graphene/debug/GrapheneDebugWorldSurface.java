package io.github.trethore.graphene.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.fabric.api.surface.BrowserView;
import io.github.trethore.graphene.fabric.api.surface.BrowserWorldSurface;
import io.github.trethore.graphene.fabric.api.surface.WorldSurfaceTransparency;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneDebugWorldSurface {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneDebugWorldSurface.class);
    private static final String URL = "https://i.pinimg.com/736x/cb/3e/01/cb3e014d6122af3b43933bb571859ae7.jpg";
    private static final double X = 0.0;
    private static final double Y = 130.0;
    private static final double Z = 0.0;

    private static BrowserView view;
    private static BrowserWorldSurface surface;
    private static boolean closed;

    private GrapheneDebugWorldSurface() {}

    static void register() {
        GrapheneDebugMinecraftAccess.registerWorldSurface();
        GrapheneDebugClient.context().runtime().initialization().whenComplete((ignored, failure) -> {
            if (failure != null) {
                LOGGER.error("Failed to initialize the debug world browser surface", failure);
                return;
            }
            Minecraft.getInstance().execute(GrapheneDebugWorldSurface::initialize);
        });
    }

    private static void initialize() {
        if (closed) {
            return;
        }
        view = BrowserView.builder(GrapheneDebugClient.context())
                .url(URL)
                .options(BrowserOptions.builder().transparent(true).build())
                .resolution(1280, 720)
                .build();
        surface = BrowserWorldSurface.builder(view)
                .dimensions(16.0F, 9.0F)
                .fullBright(true)
                .transparencyMode(WorldSurfaceTransparency.BLENDED)
                .opacity(0.5F)
                .build();
    }

    static void submit(
            SubmitNodeCollector collector, PoseStack poseStack, double cameraX, double cameraY, double cameraZ) {
        if (surface == null) {
            return;
        }

        float yaw = (float) Math.atan2(cameraX - X, cameraZ - Z);
        poseStack.pushPose();
        poseStack.translate(X - cameraX, Y - cameraY, Z - cameraZ);
        poseStack.mulPose(new Quaternionf().rotationY(yaw));
        surface.submit(collector, poseStack);
        poseStack.popPose();
    }

    static void close() {
        closed = true;
        if (view == null) {
            return;
        }
        view.close();
        view = null;
        surface = null;
    }
}
