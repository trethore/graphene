package io.github.trethore.graphene.debug;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class GrapheneDebugMinecraftAccess {
    private GrapheneDebugMinecraftAccess() {}

    static void registerKeyMapping(KeyMapping mapping) {
        KeyBindingHelper.registerKeyBinding(mapping);
    }

    static void showScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }
}
