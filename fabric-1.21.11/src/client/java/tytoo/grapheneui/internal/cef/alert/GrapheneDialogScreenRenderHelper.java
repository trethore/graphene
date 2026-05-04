package tytoo.grapheneui.internal.cef.alert;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;

final class GrapheneDialogScreenRenderHelper {
    private static final int DEFAULT_TITLE_Y = 40;

    private GrapheneDialogScreenRenderHelper() {
    }

    static void renderCenteredTitleAndMessage(
            GuiGraphics guiGraphics,
            Font font,
            Component title,
            int screenWidth,
            MultiLineLabel messageLabel,
            int titleColor,
            int textStartY,
            int textLineHeight
    ) {
        guiGraphics.drawCenteredString(font, title, screenWidth / 2, DEFAULT_TITLE_Y, titleColor);
        ActiveTextCollector activeTextCollector = guiGraphics.textRenderer();
        messageLabel.visitLines(TextAlignment.CENTER, screenWidth / 2, textStartY, textLineHeight, activeTextCollector);
    }
}
