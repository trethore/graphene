package io.github.trethore.graphene.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
@SuppressWarnings("java:S100")
public abstract class GuiGraphicsExtractorMixin implements GrapheneGuiGraphics {
    @Invoker("innerBlit")
    protected abstract void graphene$invokeInnerBlit(
            RenderPipeline pipeline,
            GpuTextureView texture,
            GpuSampler sampler,
            int x0,
            int y0,
            int x1,
            int y1,
            float u0,
            float u1,
            float v0,
            float v1,
            int color);

    @Unique
    @Override
    public int graphene$width() {
        return graphene$self().guiWidth();
    }

    @Unique
    @Override
    public int graphene$height() {
        return graphene$self().guiHeight();
    }

    @Unique
    @Override
    public void graphene$nextStratum() {
        graphene$self().nextStratum();
    }

    @Unique
    @Override
    public void graphene$fill(int x0, int y0, int x1, int y1, int color) {
        graphene$self().fill(x0, y0, x1, y1, color);
    }

    @Unique
    @Override
    public void graphene$text(Font font, String text, int x, int y, int color, boolean shadow) {
        graphene$self().text(font, text, x, y, color, shadow);
    }

    @Unique
    @Override
    public void graphene$centeredText(Font font, Component text, int x, int y, int color) {
        graphene$self().centeredText(font, text, x, y, color);
    }

    @Unique
    @Override
    public void graphene$blit(
            RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler, int x, int y, int width, int height) {
        graphene$invokeInnerBlit(pipeline, texture, sampler, x, y, x + width, y + height, 0.0F, 1.0F, 0.0F, 1.0F, -1);
    }

    @Unique
    private GuiGraphicsExtractor graphene$self() {
        return (GuiGraphicsExtractor) (Object) this;
    }
}
