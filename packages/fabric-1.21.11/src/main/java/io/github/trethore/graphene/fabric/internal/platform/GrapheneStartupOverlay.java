package io.github.trethore.graphene.fabric.internal.platform;

import io.github.trethore.graphene.fabric.internal.util.MinecraftReferences;
import net.minecraft.client.gui.GuiGraphics;
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

  private volatile String stage = "INITIALIZING";
  private volatile double progress = -1.0;

  void update(String stage, double progress) {
    this.stage = stage == null || stage.isBlank() ? "INITIALIZING" : stage;
    this.progress = progress;
  }

  @Override
  public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    Screen screen = MinecraftReferences.screen();
    if (screen != null) {
      screen.renderWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
    }
    int centerX = graphics.guiWidth() / 2;
    int barLeft = centerX - BAR_WIDTH / 2;
    int barTop = graphics.guiHeight() / 2;
    graphics.nextStratum();
    graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), BACKGROUND);
    graphics.drawCenteredString(
        MinecraftReferences.font(),
        Component.literal("Graphene: " + displayStage()),
        centerX,
        barTop - 22,
        0xFFFFFFFF);
    graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, BAR_OUTLINE);
    graphics.fill(
        barLeft + 1, barTop + 1, barLeft + BAR_WIDTH - 1, barTop + BAR_HEIGHT - 1, BAR_BACKGROUND);
    int fillWidth =
        progress < 0.0
            ? animatedFillWidth()
            : (int) Math.round((BAR_WIDTH - 2) * Math.clamp(progress, 0.0, 1.0));
    if (fillWidth > 0) {
      graphics.fill(
          barLeft + 1, barTop + 1, barLeft + 1 + fillWidth, barTop + BAR_HEIGHT - 1, BAR_FILL);
    }
  }

  private String displayStage() {
    return stage.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
  }

  private static int animatedFillWidth() {
    int available = BAR_WIDTH - 2;
    return (int) ((System.currentTimeMillis() / 8L) % available);
  }
}
