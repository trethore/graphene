package io.github.trethore.graphene.fabric.internal.platform;

import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import io.github.trethore.graphene.minecraft.internal.util.MinecraftReferences;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.NonNull;

final class GrapheneStartupOverlay extends Overlay {
    private final GrapheneStartupOverlaySupport support = new GrapheneStartupOverlaySupport();

    void update(String stage, double progress) {
        support.update(stage, progress);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Screen screen = MinecraftReferences.screen();
        if (screen != null) {
            screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
        }
        support.render((GrapheneGuiGraphics) graphics);
    }
}
