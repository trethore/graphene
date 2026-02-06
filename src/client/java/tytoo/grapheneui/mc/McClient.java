package tytoo.grapheneui.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class McClient {
    private McClient() {
    }

    public static int windowWidth() {
        return Minecraft.getInstance().getWindow().getWidth();
    }

    public static int windowHeight() {
        return Minecraft.getInstance().getWindow().getHeight();
    }

    public static int guiScaledWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int guiScaledHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static void registerTexture(Identifier textureId, DynamicTexture dynamicTexture) {
        Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
    }

    public static void releaseTexture(Identifier textureId) {
        Minecraft.getInstance().getTextureManager().release(textureId);
    }
}
