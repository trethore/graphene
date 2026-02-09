package tytoo.grapheneui.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import tytoo.grapheneui.mc.McTextures;

public final class GrapheneTexture {
    private final Identifier textureId;
    private DynamicTexture dynamicTexture;

    public GrapheneTexture(Identifier textureId) {
        this.textureId = textureId;
    }

    public boolean ensureSize(int width, int height) {
        if (dynamicTexture != null) {
            NativeImage nativeImage = dynamicTexture.getPixels();
            if (nativeImage != null && nativeImage.getWidth() == width && nativeImage.getHeight() == height) {
                return false;
            }
        }

        release();
        dynamicTexture = McTextures.createAndRegister(textureId, width, height);
        return true;
    }

    public NativeImage getPixels() {
        if (dynamicTexture == null) {
            return null;
        }

        return dynamicTexture.getPixels();
    }

    public void upload() {
        if (dynamicTexture != null) {
            dynamicTexture.upload();
        }
    }

    public void release() {
        McTextures.release(textureId);
        dynamicTexture = null;
    }

    public Identifier textureId() {
        return textureId;
    }
}
