package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.Objects;

public final class GrapheneBrowserSurfaceState implements AutoCloseable {
    private final BrowserSession browser;
    private final GrapheneSurfaceSizingState sizing;
    private boolean closed;

    public GrapheneBrowserSurfaceState(
            GrapheneContext context,
            String url,
            BrowserOptions options,
            int width,
            int height,
            boolean autoResolution,
            int resolutionWidth,
            int resolutionHeight,
            double scaleFactor) {
        GrapheneContext validatedContext = Objects.requireNonNull(context, "context");
        sizing = new GrapheneSurfaceSizingState(
                width, height, autoResolution, resolutionWidth, resolutionHeight, scaleFactor);
        browser = validatedContext
                .browsers()
                .create(
                        Objects.requireNonNull(url, "url"),
                        Objects.requireNonNull(options, "options"),
                        sizing.resolutionWidth(),
                        sizing.resolutionHeight());
    }

    public BrowserSession browser() {
        return browser;
    }

    public int width() {
        return sizing.width();
    }

    public int height() {
        return sizing.height();
    }

    public int resolutionWidth() {
        return sizing.resolutionWidth();
    }

    public int resolutionHeight() {
        return sizing.resolutionHeight();
    }

    public boolean autoResolution() {
        return sizing.autoResolution();
    }

    public void resize(int width, int height, double scaleFactor) {
        ensureOpen();
        applyResize(sizing.resize(width, height, scaleFactor));
    }

    public void setResolution(int width, int height) {
        ensureOpen();
        applyResize(sizing.setResolution(width, height));
    }

    public void useAutoResolution(double scaleFactor) {
        ensureOpen();
        applyResize(sizing.useAutoResolution(scaleFactor));
    }

    public int mapX(double coordinate, int renderedWidth) {
        return sizing.mapX(coordinate, renderedWidth);
    }

    public int mapY(double coordinate, int renderedHeight) {
        return sizing.mapY(coordinate, renderedHeight);
    }

    public boolean isClosed() {
        return closed;
    }

    public void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("BrowserSurface is closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        browser.close();
    }

    private void applyResize(GrapheneSurfaceSizingState.Resize resize) {
        if (resize.required()) {
            browser.resize(resize.width(), resize.height());
        }
    }
}
