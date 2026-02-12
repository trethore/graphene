package tytoo.grapheneui.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Objects;

@SuppressWarnings({"resource", "java:S2095"})
public final class McClient {
    private McClient() {
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static void execute(Runnable runnable) {
        mc().execute(Objects.requireNonNull(runnable, "runnable"));
    }

    public static Screen currentScreen() {
        return mc().screen;
    }

    public static void setScreen(Screen screen) {
        mc().setScreen(screen);
    }

    public static int windowWidth() {
        return mc().getWindow().getWidth();
    }

    public static int windowHeight() {
        return mc().getWindow().getHeight();
    }

    public static int guiScaledWidth() {
        return mc().getWindow().getGuiScaledWidth();
    }

    public static int guiScaledHeight() {
        return mc().getWindow().getGuiScaledHeight();
    }

    public static void registerTexture(Identifier textureId, DynamicTexture dynamicTexture) {
        mc().getTextureManager().register(textureId, dynamicTexture);
    }

    public static void releaseTexture(Identifier textureId) {
        mc().getTextureManager().release(textureId);
    }
}
