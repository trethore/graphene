package tytoo.grapheneui.internal.mc;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class McTextures {
    private McTextures() {
    }

    public static DynamicTexture createAndRegister(Identifier textureId, int width, int height) {
        DynamicTexture dynamicTexture = new DynamicTexture(textureId::toString, width, height, true);
        McClient.registerTexture(textureId, dynamicTexture);
        return dynamicTexture;
    }

    public static void release(Identifier textureId) {
        McClient.releaseTexture(textureId);
    }
}
