package io.github.trethore.graphene.debug;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

final class GrapheneDebugMinecraftAccess {
    private GrapheneDebugMinecraftAccess() {}

    static void registerKeyMapping(KeyMapping mapping) {
        KeyMappingHelper.registerKeyMapping(mapping);
    }

    static void showScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreenAndShow(screen);
    }

    static void registerWorldSurface() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            Vec3 cameraPosition = context.levelState().cameraRenderState.pos;
            GrapheneDebugWorldSurface.submit(
                    context.submitNodeCollector(),
                    context.poseStack(),
                    cameraPosition.x,
                    cameraPosition.y,
                    cameraPosition.z);
        });
    }
}
