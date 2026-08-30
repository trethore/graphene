package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

final class GrapheneBrowserGpuTextureFactory {
    private GrapheneBrowserGpuTextureFactory() {}

    static GpuTexture create(int width, int height) {
        return RenderSystem.getDevice()
                .createTexture(
                        () -> "Graphene Browser",
                        GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                        TextureFormat.RGBA8,
                        width,
                        height,
                        1,
                        1);
    }
}
