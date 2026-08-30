package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.Objects;

public final class GrapheneBrowserViewState implements AutoCloseable {
    private final BrowserSession browser;
    private int resolutionWidth;
    private int resolutionHeight;
    private boolean closed;

    public GrapheneBrowserViewState(
            GrapheneContext context, String url, BrowserOptions options, int resolutionWidth, int resolutionHeight) {
        GrapheneContext validatedContext = Objects.requireNonNull(context, "context");
        this.resolutionWidth = requirePositive(resolutionWidth, "resolutionWidth");
        this.resolutionHeight = requirePositive(resolutionHeight, "resolutionHeight");
        browser = validatedContext
                .browsers()
                .create(
                        Objects.requireNonNull(url, "url"),
                        Objects.requireNonNull(options, "options"),
                        resolutionWidth,
                        resolutionHeight);
    }

    public BrowserSession browser() {
        return browser;
    }

    public int resolutionWidth() {
        return resolutionWidth;
    }

    public int resolutionHeight() {
        return resolutionHeight;
    }

    public void setResolution(int width, int height) {
        ensureOpen();
        int validatedWidth = requirePositive(width, "width");
        int validatedHeight = requirePositive(height, "height");
        if (validatedWidth == resolutionWidth && validatedHeight == resolutionHeight) {
            return;
        }
        resolutionWidth = validatedWidth;
        resolutionHeight = validatedHeight;
        browser.resize(width, height);
    }

    public int mapX(double normalizedX) {
        return mapCoordinate(normalizedX, resolutionWidth);
    }

    public int mapY(double normalizedY) {
        return mapCoordinate(normalizedY, resolutionHeight);
    }

    public boolean isClosed() {
        return closed;
    }

    public void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("BrowserView is closed");
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

    private static int mapCoordinate(double coordinate, int resolution) {
        if (!Double.isFinite(coordinate)) {
            throw new IllegalArgumentException("normalized coordinate must be finite");
        }
        double normalized = Math.clamp(coordinate, 0.0, 1.0);
        return Math.min((int) Math.floor(normalized * resolution), resolution - 1);
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
