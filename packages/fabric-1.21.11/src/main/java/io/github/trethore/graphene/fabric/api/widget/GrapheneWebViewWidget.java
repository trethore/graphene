package io.github.trethore.graphene.fabric.api.widget;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.fabric.api.surface.BrowserGuiSurface;
import io.github.trethore.graphene.fabric.api.surface.BrowserSurface;
import io.github.trethore.graphene.fabric.api.surface.BrowserView;
import net.minecraft.client.gui.GuiGraphics;
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

    /**
     * Creates a widget that takes ownership of {@code view} and closes it when the widget closes.
     */
    public GrapheneWebViewWidget(
            Screen screen, int x, int y, int width, int height, Component message, BrowserView view) {
        super(screen, x, y, width, height, message, view);
    }

    /**
     * Creates a widget that takes ownership of the supplied surface's view and closes the view when
     * the widget closes.
     */
    public GrapheneWebViewWidget(
            Screen screen, int x, int y, int width, int height, Component message, BrowserGuiSurface surface) {
        super(screen, x, y, width, height, message, surface);
    }

    /**
     * Creates a widget that takes ownership of {@code surface} and closes it when the widget closes.
     *
     * @deprecated Use the {@link BrowserView} or {@link BrowserGuiSurface} constructor.
     */
    @Deprecated(since = "2.3.0")
    public GrapheneWebViewWidget(
            Screen screen, int x, int y, int width, int height, Component message, BrowserSurface surface) {
        super(screen, x, y, width, height, message, surface);
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
        inputAdapter().text(event.codepointAsString(), event.modifiers());
        return true;
    }
}
