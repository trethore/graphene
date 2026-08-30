package io.github.trethore.graphene.fabric.internal.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public interface GrapheneGuiGraphics {
    int graphene$width();

    int graphene$height();

    void graphene$nextStratum();

    void graphene$fill(int x0, int y0, int x1, int y1, int color);

    void graphene$text(Font font, String text, int x, int y, int color, boolean shadow);

    void graphene$centeredText(Font font, Component text, int x, int y, int color);

    void graphene$blit(
            RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler, int x, int y, int width, int height);
}
