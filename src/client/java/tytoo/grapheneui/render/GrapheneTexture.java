package tytoo.grapheneui.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

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
        dynamicTexture = new DynamicTexture(textureId::toString, width, height, true);
        Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
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
        Minecraft.getInstance().getTextureManager().release(textureId);
        dynamicTexture = null;
    }

    public Identifier textureId() {
        return textureId;
    }
}
