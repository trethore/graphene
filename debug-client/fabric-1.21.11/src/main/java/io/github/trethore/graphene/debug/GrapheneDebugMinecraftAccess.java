package io.github.trethore.graphene.debug;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

final class GrapheneDebugMinecraftAccess {
    private GrapheneDebugMinecraftAccess() {}

    static void registerKeyMapping(KeyMapping mapping) {
        KeyBindingHelper.registerKeyBinding(mapping);
    }

    static void showScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }

    static void registerWorldSurface() {
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            Vec3 cameraPosition = context.worldState().cameraRenderState.pos;
            GrapheneDebugWorldSurface.submit(
                    context.commandQueue(), context.matrices(), cameraPosition.x, cameraPosition.y, cameraPosition.z);
        });
    }
}
