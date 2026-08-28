package io.github.trethore.graphene.minecraft.internal.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

final class MinecraftClientAccess {
    private MinecraftClientAccess() {}

    static Screen screen(Minecraft client) {
        return client.gui.screen();
    }

    static Overlay overlay(Minecraft client) {
        return client.gui.overlay();
    }

    static void setOverlay(Minecraft client, Overlay overlay) {
        client.gui.setOverlay(overlay);
    }
}
