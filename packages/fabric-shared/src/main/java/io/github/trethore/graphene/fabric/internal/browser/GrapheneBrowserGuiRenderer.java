package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

public final class GrapheneBrowserGuiRenderer {
    public void render(
            GrapheneGuiGraphics graphics, GrapheneBrowserGpuTexture texture, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.graphene$blit(
                RenderPipelines.GUI_TEXTURED,
                texture.view(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
                x,
                y,
                width,
                height);
    }
}
