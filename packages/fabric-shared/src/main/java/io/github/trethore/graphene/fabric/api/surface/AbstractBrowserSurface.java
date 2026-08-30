package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserGpuRenderer;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import io.github.trethore.graphene.internal.browser.GrapheneBrowserSurfaceState;
import io.github.trethore.graphene.minecraft.internal.util.MinecraftReferences;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractBrowserSurface implements AutoCloseable {
    private final GrapheneBrowserSurfaceState state;
    private final GrapheneBrowserGpuRenderer renderer = new GrapheneBrowserGpuRenderer();

    protected AbstractBrowserSurface(Builder builder) {
        state = new GrapheneBrowserSurfaceState(
                builder.context,
                builder.url,
                builder.options,
                builder.width,
                builder.height,
                builder.autoResolution,
                builder.resolutionWidth,
                builder.resolutionHeight,
                MinecraftReferences.guiScale());
    }

    public final BrowserSession browser() {
        return state.browser();
    }

    public final int width() {
        return state.width();
    }

    public final int height() {
        return state.height();
    }

    public final int resolutionWidth() {
        return state.resolutionWidth();
    }

    public final int resolutionHeight() {
        return state.resolutionHeight();
    }

    public final boolean isAutoResolution() {
        return state.autoResolution();
    }

    public final void resize(int width, int height) {
        state.resize(width, height, MinecraftReferences.guiScale());
    }

    public final void setResolution(int width, int height) {
        state.setResolution(width, height);
    }

    public final void useAutoResolution() {
        state.useAutoResolution(MinecraftReferences.guiScale());
    }

    public final int toBrowserX(double surfaceX, int renderedWidth) {
        return state.mapX(surfaceX, renderedWidth);
    }

    public final int toBrowserY(double surfaceY, int renderedHeight) {
        return state.mapY(surfaceY, renderedHeight);
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }
        renderer.close();
        state.close();
    }

    protected final boolean isClosed() {
        return state.isClosed();
    }

    protected final void ensureOpen() {
        state.ensureOpen();
    }

    protected final void render(GrapheneGuiGraphics graphics, int x, int y) {
        render(graphics, x, y, width(), height());
    }

    @SuppressWarnings("resource")
    protected final void render(GrapheneGuiGraphics graphics, int x, int y, int renderedWidth, int renderedHeight) {
        ensureOpen();
        GrapheneGuiGraphics validatedGraphics = Objects.requireNonNull(graphics, "graphics");
        int validatedWidth = requirePositive(renderedWidth, "renderedWidth");
        int validatedHeight = requirePositive(renderedHeight, "renderedHeight");
        Optional<BrowserFrame> availableFrame = browser().latestFrame();
        if (availableFrame.isEmpty()) {
            return;
        }
        renderer.render(
                validatedGraphics,
                availableFrame.get(),
                browser().options().transparent(),
                x,
                y,
                validatedWidth,
                validatedHeight);
    }

    protected static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    protected static class Builder {
        private final GrapheneContext context;
        private String url = "about:blank";
        private BrowserOptions options = BrowserOptions.defaults();
        private int width = 1;
        private int height = 1;
        private boolean autoResolution = true;
        private int resolutionWidth = 1;
        private int resolutionHeight = 1;

        protected Builder(GrapheneContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        protected final void setUrl(String url) {
            this.url = Objects.requireNonNull(url, "url");
        }

        protected final void setOptions(BrowserOptions options) {
            this.options = Objects.requireNonNull(options, "options");
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
