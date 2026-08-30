package io.github.trethore.graphene.fabric.api.surface;

import java.util.Objects;

/** Maps Minecraft window coordinates into a browser GUI surface and forwards browser input. */
@SuppressWarnings("unused")
public final class BrowserGuiSurfaceInputAdapter implements AutoCloseable {
    private final BrowserGuiSurface surface;
    private final BrowserViewInputAdapter input;

    public BrowserGuiSurfaceInputAdapter(BrowserGuiSurface surface) {
        this.surface = Objects.requireNonNull(surface, "surface");
        input = new BrowserViewInputAdapter(surface.view());
    }

    public void setFocused(boolean focused) {
        input.setFocused(focused);
    }

    public void mouseMoved(
            double mouseX,
            double mouseY,
            int surfaceX,
            int surfaceY,
            int renderedWidth,
            int renderedHeight,
            int modifiers) {
        input.mouseMoved(
                surface.toBrowserX(mouseX - surfaceX, renderedWidth),
                surface.toBrowserY(mouseY - surfaceY, renderedHeight),
                modifiers);
    }

    public void mouseButton(
            double mouseX,
            double mouseY,
            int surfaceX,
            int surfaceY,
            int renderedWidth,
            int renderedHeight,
            int button,
            boolean pressed,
            int clickCount,
            int modifiers) {
        input.mouseButton(
                surface.toBrowserX(mouseX - surfaceX, renderedWidth),
                surface.toBrowserY(mouseY - surfaceY, renderedHeight),
                button,
                pressed,
                clickCount,
                modifiers);
    }

    public void mouseDragged(
            double mouseX,
            double mouseY,
            int surfaceX,
            int surfaceY,
            int renderedWidth,
            int renderedHeight,
            int modifiers) {
        input.mouseDragged(
                surface.toBrowserX(mouseX - surfaceX, renderedWidth),
                surface.toBrowserY(mouseY - surfaceY, renderedHeight),
                modifiers);
    }

    public void mouseScrolled(
            double mouseX,
            double mouseY,
            int surfaceX,
            int surfaceY,
            int renderedWidth,
            int renderedHeight,
            double horizontal,
            double vertical,
            int modifiers) {
        input.mouseScrolled(
                surface.toBrowserX(mouseX - surfaceX, renderedWidth),
                surface.toBrowserY(mouseY - surfaceY, renderedHeight),
                horizontal,
                vertical,
                modifiers);
    }

    public void key(int keyCode, int scanCode, boolean pressed, int modifiers) {
        input.key(keyCode, scanCode, pressed, modifiers);
    }

    public void text(String text, int modifiers) {
        input.text(text, modifiers);
    }

    public void text(String text) {
        input.text(text);
    }

    @Override
    public void close() {
        input.close();
    }
}
