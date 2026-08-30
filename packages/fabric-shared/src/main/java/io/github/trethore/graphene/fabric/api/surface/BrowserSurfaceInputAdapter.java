package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.internal.platform.GrapheneClipboardContent;

/**
 * Compatibility input adapter for {@link BrowserSurface}.
 *
 * @deprecated Use {@link BrowserGuiSurfaceInputAdapter}.
 */
@Deprecated(since = "2.3.0")
@SuppressWarnings("unused")
public final class BrowserSurfaceInputAdapter implements AutoCloseable {
    private final BrowserGuiSurfaceInputAdapter input;

    public BrowserSurfaceInputAdapter(BrowserSurface surface) {
        input = new BrowserGuiSurfaceInputAdapter(surface.guiSurface());
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
        input.mouseMoved(mouseX, mouseY, surfaceX, surfaceY, renderedWidth, renderedHeight, modifiers);
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
                mouseX,
                mouseY,
                surfaceX,
                surfaceY,
                renderedWidth,
                renderedHeight,
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
        input.mouseDragged(mouseX, mouseY, surfaceX, surfaceY, renderedWidth, renderedHeight, modifiers);
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
                mouseX, mouseY, surfaceX, surfaceY, renderedWidth, renderedHeight, horizontal, vertical, modifiers);
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

    static GrapheneClipboardContent resolveClipboardContent(GrapheneClipboardContent richContent, String nativeText) {
        return BrowserViewInputAdapter.resolveClipboardContent(richContent, nativeText);
    }

    static boolean isPasteShortcut(int keyCode, int modifiers) {
        return BrowserViewInputAdapter.isPasteShortcut(keyCode, modifiers);
    }

    static boolean isClipboardWriteShortcut(int keyCode, int modifiers) {
        return BrowserViewInputAdapter.isClipboardWriteShortcut(keyCode, modifiers);
    }
}
