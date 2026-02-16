package tytoo.grapheneui.api.render;

import net.minecraft.resources.Identifier;

/**
 * Interface representing a render target for Graphene UI. This abstraction allows rendering web content onto various
 * types of targets, such as Minecraft GUIs or off-screen buffers. Implementations of this interface are responsible
 * for handling the actual drawing operations and managing resources associated with the render target.
 */

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
