package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.internal.browser.GrapheneBrowserSurfaceState;
import io.github.trethore.graphene.minecraft.internal.util.MinecraftReferences;

abstract class AbstractBrowserSurface implements AutoCloseable {
    private final GrapheneBrowserSurfaceState state;

    protected AbstractBrowserSurface(
            GrapheneContext context,
            String url,
            BrowserOptions options,
            int width,
            int height,
            boolean autoResolution,
            int resolutionWidth,
            int resolutionHeight) {
        state = new GrapheneBrowserSurfaceState(
                context,
                url,
                options,
                width,
                height,
                autoResolution,
                resolutionWidth,
                resolutionHeight,
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
        state.close();
    }

    protected final boolean isClosed() {
        return state.isClosed();
    }

    protected final void ensureOpen() {
        state.ensureOpen();
    }

    protected static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
