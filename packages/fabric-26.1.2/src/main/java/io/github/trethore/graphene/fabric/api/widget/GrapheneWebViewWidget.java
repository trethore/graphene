package io.github.trethore.graphene.fabric.api.widget;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.fabric.api.surface.BrowserSurface;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class GrapheneWebViewWidget extends AbstractGrapheneWebViewWidget {
    public GrapheneWebViewWidget(
            GrapheneContext context,
            Screen screen,
            int x,
            int y,
            int width,
            int height,
            Component message,
            String url) {
        super(context, screen, x, y, width, height, message, url);
    }

    public GrapheneWebViewWidget(
            Screen screen, int x, int y, int width, int height, Component message, BrowserSurface surface) {
        super(screen, x, y, width, height, message, surface);
    }

    @Override
    @SuppressWarnings("resource")
    protected void extractWidgetRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (isMouseOver(mouseX, mouseY)) {
            updatePointerPosition(mouseX, mouseY);
        }
        surface().render(graphics, getX(), getY(), getWidth(), getHeight());
        if (isMouseOver(mouseX, mouseY)) {
            graphics.requestCursor(cursor());
        }
    }

    @Override
    @SuppressWarnings("resource")
    public boolean charTyped(CharacterEvent event) {
        inputAdapter().text(event.codepointAsString());
        return true;
    }
}
