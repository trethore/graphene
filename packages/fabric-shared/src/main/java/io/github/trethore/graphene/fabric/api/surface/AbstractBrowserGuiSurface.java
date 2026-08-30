package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserGuiRenderer;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import io.github.trethore.graphene.internal.browser.GrapheneSurfaceSizingState;
import io.github.trethore.graphene.minecraft.internal.util.MinecraftReferences;
import java.util.Objects;

abstract class AbstractBrowserGuiSurface {
    private final BrowserView view;
    private final GrapheneSurfaceSizingState sizing;
    private final GrapheneBrowserGuiRenderer renderer = new GrapheneBrowserGuiRenderer();

    protected AbstractBrowserGuiSurface(Builder builder) {
        view = builder.view;
        sizing = new GrapheneSurfaceSizingState(
                builder.width,
                builder.height,
                builder.autoResolution,
                builder.resolutionWidth,
                builder.resolutionHeight,
                MinecraftReferences.guiScale());
        view.setResolution(sizing.resolutionWidth(), sizing.resolutionHeight());
    }

    public final BrowserView view() {
        return view;
    }

    public final BrowserSession browser() {
        return view.browser();
    }

    public final int width() {
        return sizing.width();
    }

    public final int height() {
        return sizing.height();
    }

    public final int resolutionWidth() {
        return view.resolutionWidth();
    }

    public final int resolutionHeight() {
        return view.resolutionHeight();
    }

    public final boolean isAutoResolution() {
        return sizing.autoResolution();
    }

    public final void resize(int width, int height) {
        applyResize(sizing.resize(width, height, MinecraftReferences.guiScale()));
    }

    public final void setResolution(int width, int height) {
        applyResize(sizing.setResolution(width, height));
    }

    public final void useAutoResolution() {
        applyResize(sizing.useAutoResolution(MinecraftReferences.guiScale()));
    }

    public final int toBrowserX(double surfaceX, int renderedWidth) {
        return sizing.mapX(surfaceX, renderedWidth);
    }

    public final int toBrowserY(double surfaceY, int renderedHeight) {
        return sizing.mapY(surfaceY, renderedHeight);
    }

    protected final void render(GrapheneGuiGraphics graphics, int x, int y) {
        render(graphics, x, y, width(), height());
    }

    protected final void render(GrapheneGuiGraphics graphics, int x, int y, int renderedWidth, int renderedHeight) {
        GrapheneGuiGraphics validatedGraphics = Objects.requireNonNull(graphics, "graphics");
        int validatedWidth = requirePositive(renderedWidth, "renderedWidth");
        int validatedHeight = requirePositive(renderedHeight, "renderedHeight");
        view.prepareTexture()
                .ifPresent(
                        texture -> renderer.render(validatedGraphics, texture, x, y, validatedWidth, validatedHeight));
    }

    protected static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private void applyResize(GrapheneSurfaceSizingState.Resize resize) {
        if (resize.required()) {
            view.setResolution(resize.width(), resize.height());
        }
    }

    protected static class Builder {
        private final BrowserView view;
        private int width = 1;
        private int height = 1;
        private boolean autoResolution = true;
        private int resolutionWidth = 1;
        private int resolutionHeight = 1;

        protected Builder(BrowserView view) {
            this.view = Objects.requireNonNull(view, "view");
        }

        protected final void setSize(int width, int height) {
            this.width = requirePositive(width, "width");
            this.height = requirePositive(height, "height");
        }

        protected final void setResolution(int width, int height) {
            autoResolution = false;
            resolutionWidth = requirePositive(width, "resolutionWidth");
            resolutionHeight = requirePositive(height, "resolutionHeight");
        }

        protected final void enableAutoResolution() {
            autoResolution = true;
        }
    }
}
