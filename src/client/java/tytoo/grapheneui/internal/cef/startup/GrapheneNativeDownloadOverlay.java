package tytoo.grapheneui.internal.cef.startup;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.internal.mc.McClient;

import java.util.Objects;

public final class GrapheneNativeDownloadOverlay extends Overlay {
    private static final int FULL_OPACITY = 255;
    private static final int BACKGROUND_COLOR = ARGB.color(200, 34, 34, 34);
    private static final int BAR_OUTLINE_COLOR = ARGB.color(FULL_OPACITY, 64, 64, 74);
    private static final int BAR_BACKGROUND_COLOR = ARGB.color(FULL_OPACITY, 24, 24, 30);
    private static final int BAR_FILL_COLOR = ARGB.color(FULL_OPACITY, 76, 175, 80);
    private static final int TITLE_COLOR = ARGB.color(FULL_OPACITY, 255, 255, 255);
    private static final int BAR_WIDTH = 240;
    private static final int BAR_HEIGHT = 14;
    private static final int BORDER_WIDTH = 1;
    private static final int TITLE_MARGIN = 18;

    private final GrapheneNativeDownloadState state;

    public GrapheneNativeDownloadOverlay(GrapheneNativeDownloadState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!state.isActive()) {
            return;
        }

        Screen currentScreen = McClient.currentScreen();
        if (currentScreen != null) {
            currentScreen.renderWithTooltipAndSubtitles(guiGraphics, mouseX, mouseY, partialTick);
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int barLeft = width / 2 - BAR_WIDTH / 2;
        int barTop = height / 2;
        int filledWidth = Math.round((BAR_WIDTH - BORDER_WIDTH * 2) * state.progress());
        Font font = McClient.mc().font;
        Component title = Component.literal("Graphene: downloading natives for " + state.platformIdentifier());

        guiGraphics.nextStratum();
        guiGraphics.fillGradient(0, 0, width, height, BACKGROUND_COLOR, BACKGROUND_COLOR);
        guiGraphics.drawCenteredString(
                font,
                title,
                width / 2,
                barTop - TITLE_MARGIN - font.lineHeight,
                TITLE_COLOR
        );
        guiGraphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, BAR_OUTLINE_COLOR);
        guiGraphics.fill(
                barLeft + BORDER_WIDTH,
                barTop + BORDER_WIDTH,
                barLeft + BAR_WIDTH - BORDER_WIDTH,
                barTop + BAR_HEIGHT - BORDER_WIDTH,
                BAR_BACKGROUND_COLOR
        );
        if (filledWidth > 0) {
            guiGraphics.fill(
                    barLeft + BORDER_WIDTH,
                    barTop + BORDER_WIDTH,
                    barLeft + BORDER_WIDTH + filledWidth,
                    barTop + BAR_HEIGHT - BORDER_WIDTH,
                    BAR_FILL_COLOR
            );
        }
    }

    @Override
    public void tick() {
        if (!state.isActive() && McClient.currentOverlay() == this) {
            McClient.setOverlay(null);
        }
    }
}
