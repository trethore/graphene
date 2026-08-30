package io.github.trethore.graphene.fabric.api.surface;

import java.util.Objects;

/** Forwards input at a previously computed browser world-surface hit. */
@SuppressWarnings("unused")
public final class BrowserWorldSurfaceInputAdapter implements AutoCloseable {
    private final BrowserWorldSurface surface;
    private final BrowserViewInputAdapter input;

    public BrowserWorldSurfaceInputAdapter(BrowserWorldSurface surface) {
        this.surface = Objects.requireNonNull(surface, "surface");
        input = new BrowserViewInputAdapter(surface.view());
    }

    public BrowserWorldSurface surface() {
        return surface;
    }

    public void setFocused(boolean focused) {
        input.setFocused(focused);
    }

    public void mouseMoved(BrowserWorldSurfaceHit hit, int modifiers) {
        BrowserWorldSurfaceHit validatedHit = Objects.requireNonNull(hit, "hit");
        input.mouseMoved(validatedHit.browserX(), validatedHit.browserY(), modifiers);
    }

    public void mouseButton(BrowserWorldSurfaceHit hit, int button, boolean pressed, int clickCount, int modifiers) {
        BrowserWorldSurfaceHit validatedHit = Objects.requireNonNull(hit, "hit");
        input.mouseButton(validatedHit.browserX(), validatedHit.browserY(), button, pressed, clickCount, modifiers);
    }

    public void mouseDragged(BrowserWorldSurfaceHit hit, int modifiers) {
        BrowserWorldSurfaceHit validatedHit = Objects.requireNonNull(hit, "hit");
        input.mouseDragged(validatedHit.browserX(), validatedHit.browserY(), modifiers);
    }

    public void mouseScrolled(BrowserWorldSurfaceHit hit, double horizontal, double vertical, int modifiers) {
        BrowserWorldSurfaceHit validatedHit = Objects.requireNonNull(hit, "hit");
        input.mouseScrolled(validatedHit.browserX(), validatedHit.browserY(), horizontal, vertical, modifiers);
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
