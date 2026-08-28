package io.github.trethore.graphene.minecraft.internal.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

final class MinecraftClientAccess {
    private MinecraftClientAccess() {}

    static Screen screen(Minecraft client) {
        return client.screen;
    }

    static Overlay overlay(Minecraft client) {
        return client.getOverlay();
    }

    static void setOverlay(Minecraft client, Overlay overlay) {
        client.setOverlay(overlay);
    }
}
