package tytoo.grapheneui.api.render;

import net.minecraft.resources.Identifier;

public interface GrapheneRenderTarget {
    default void blitTexture(
            Identifier textureId,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        blitTextureRegion(textureId, x, y, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    void blitTextureRegion(
            Identifier textureId,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            int textureWidth,
            int textureHeight
    );
}
