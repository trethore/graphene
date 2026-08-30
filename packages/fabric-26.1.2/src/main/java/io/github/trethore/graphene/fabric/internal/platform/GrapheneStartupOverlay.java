package io.github.trethore.graphene.fabric.internal.platform;

import io.github.trethore.graphene.internal.platform.GrapheneStartupProgress;
import io.github.trethore.graphene.minecraft.internal.util.MinecraftReferences;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

final class GrapheneStartupOverlay extends Overlay {
    private static final int BACKGROUND = 0xC8222222;
    private static final int BAR_OUTLINE = 0xFF40404A;
    private static final int BAR_BACKGROUND = 0xFF18181E;
    private static final int BAR_FILL = 0xFF4CAF50;
    private static final int BAR_WIDTH = 240;
    private static final int BAR_HEIGHT = 14;

    private final GrapheneStartupProgress progress = new GrapheneStartupProgress();

    void update(String stage, double progress) {
        this.progress.update(stage, progress);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Screen screen = MinecraftReferences.screen();
        if (screen != null) {
            screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
        }
        int centerX = graphics.guiWidth() / 2;
        int barLeft = centerX - BAR_WIDTH / 2;
        int barTop = graphics.guiHeight() / 2;
        graphics.nextStratum();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), BACKGROUND);
        graphics.centeredText(
                MinecraftReferences.font(),
                Component.literal("Graphene: " + progress.displayStage()),
                centerX,
                barTop - 22,
                0xFFFFFFFF);
        graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, BAR_OUTLINE);
        graphics.fill(barLeft + 1, barTop + 1, barLeft + BAR_WIDTH - 1, barTop + BAR_HEIGHT - 1, BAR_BACKGROUND);
        int fillWidth = progress.fillWidth(BAR_WIDTH - 2, System.currentTimeMillis());
        if (fillWidth > 0) {
            graphics.fill(barLeft + 1, barTop + 1, barLeft + 1 + fillWidth, barTop + BAR_HEIGHT - 1, BAR_FILL);
        }
    }
}
