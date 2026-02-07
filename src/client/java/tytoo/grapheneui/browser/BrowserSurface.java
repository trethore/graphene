package tytoo.grapheneui.browser;

import net.minecraft.client.gui.GuiGraphics;
import org.cef.CefClient;
import org.cef.browser.CefRequestContext;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.cef.GrapheneCefRuntime;
import tytoo.grapheneui.mc.McWindowScale;
import tytoo.grapheneui.render.GrapheneLwjglRenderer;
import tytoo.grapheneui.render.GrapheneRenderer;

import java.awt.*;
import java.util.Objects;

@SuppressWarnings("unused") // Public API
public final class BrowserSurface implements AutoCloseable {
    private static final int MIN_SIZE = 1;
    private static final String SURFACE_WIDTH_NAME = "surfaceWidth";
    private static final String SURFACE_HEIGHT_NAME = "surfaceHeight";

    private final GrapheneBrowser browser;
    private final Rectangle viewBox = new Rectangle(0, 0, 1, 1);
    private int surfaceWidth;
    private int surfaceHeight;
    private int resolutionWidth;
    private int resolutionHeight;
    private boolean autoResolution;
    private boolean customViewBox;
    private boolean closed;

    private BrowserSurface(Builder builder) {
        this.surfaceWidth = requirePositive(builder.surfaceWidth, SURFACE_WIDTH_NAME);
        this.surfaceHeight = requirePositive(builder.surfaceHeight, SURFACE_HEIGHT_NAME);
        this.autoResolution = builder.autoResolution;

        if (autoResolution) {
            updateResolutionFromSurface();
        } else {
            setResolutionInternal(builder.resolutionWidth, builder.resolutionHeight);
        }

        if (builder.viewBox != null) {
            customViewBox = true;
            setViewBoxInternal(builder.viewBox);
        } else {
            syncViewBoxToResolution();
        }

        CefClient cefClient = builder.client != null ? builder.client : GrapheneCefRuntime.requireClient();
        CefRequestContext requestContext = builder.requestContext != null ? builder.requestContext : CefRequestContext.getGlobalContext();
        GrapheneRenderer renderer = builder.renderer != null ? builder.renderer : new GrapheneLwjglRenderer(builder.transparent);

        this.browser = new GrapheneBrowser(cefClient, builder.url, builder.transparent, requestContext, renderer);
        this.browser.createImmediately();
        this.browser.wasResizedTo(resolutionWidth, resolutionHeight);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static int mapCoordinate(double coordinate, int renderedSize, int sourceStart, int sourceSize) {
        if (renderedSize <= 0 || sourceSize <= 0) {
            return sourceStart;
        }

        double scaledCoordinate = coordinate * sourceSize / renderedSize;
        return sourceStart + (int) scaledCoordinate;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
        return value;
    }

    public GrapheneBrowser browser() {
        return browser;
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public int getResolutionWidth() {
        return resolutionWidth;
    }

    public int getResolutionHeight() {
        return resolutionHeight;
    }

    public Rectangle getViewBox() {
        return new Rectangle(viewBox);
    }

    public boolean isAutoResolution() {
        return autoResolution;
    }

    public BrowserSurface registerTo(Object owner) {
        return GrapheneCore.surfaces().register(owner, this);
    }

    public void unregisterFrom(Object owner) {
        GrapheneCore.surfaces().unregister(owner, this);
    }

    public void setSurfaceSize(int width, int height) {
        int validatedWidth = requirePositive(width, SURFACE_WIDTH_NAME);
        int validatedHeight = requirePositive(height, SURFACE_HEIGHT_NAME);

        surfaceWidth = validatedWidth;
        surfaceHeight = validatedHeight;

        if (autoResolution) {
            updateResolutionFromSurface();
            browser.wasResizedTo(resolutionWidth, resolutionHeight);
        }
    }

    public void setResolution(int width, int height) {
        autoResolution = false;
        setResolutionInternal(width, height);
        browser.wasResizedTo(resolutionWidth, resolutionHeight);
    }

    public void useAutoResolution() {
        autoResolution = true;
        updateResolutionFromSurface();
        browser.wasResizedTo(resolutionWidth, resolutionHeight);
    }

    public void setViewBox(int x, int y, int width, int height) {
        customViewBox = true;
        setViewBoxInternal(new Rectangle(x, y, width, height));
    }

    public void resetViewBox() {
        customViewBox = false;
        syncViewBoxToResolution();
    }

    public Point toBrowserPoint(double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        int browserX = toBrowserX(surfaceX, renderedWidth);
        int browserY = toBrowserY(surfaceY, renderedHeight);
        return new Point(browserX, browserY);
    }

    public int toBrowserX(double surfaceX, int renderedWidth) {
        return mapCoordinate(surfaceX, renderedWidth, viewBox.x, viewBox.width);
    }

    public int toBrowserY(double surfaceY, int renderedHeight) {
        return mapCoordinate(surfaceY, renderedHeight, viewBox.y, viewBox.height);
    }

    public void updateFrame() {
        browser.updateRendererFrame();
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        render(guiGraphics, x, y, surfaceWidth, surfaceHeight);
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        browser.renderTo(x, y, width, height, viewBox.x, viewBox.y, viewBox.width, viewBox.height, guiGraphics);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        browser.close();
    }

    private void setResolutionInternal(int width, int height) {
        this.resolutionWidth = requirePositive(width, "resolutionWidth");
        this.resolutionHeight = requirePositive(height, "resolutionHeight");

        if (customViewBox) {
            clampViewBoxToResolution();
        } else {
            syncViewBoxToResolution();
        }
    }

    private void updateResolutionFromSurface() {
        int calculatedWidth = (int) Math.max(MIN_SIZE, Math.round(surfaceWidth * McWindowScale.getScaleX()));
        int calculatedHeight = (int) Math.max(MIN_SIZE, Math.round(surfaceHeight * McWindowScale.getScaleY()));
        setResolutionInternal(calculatedWidth, calculatedHeight);
    }

    private void syncViewBoxToResolution() {
        viewBox.setBounds(0, 0, resolutionWidth, resolutionHeight);
    }

    private void setViewBoxInternal(Rectangle requestedViewBox) {
        int validatedWidth = requirePositive(requestedViewBox.width, "viewBoxWidth");
        int validatedHeight = requirePositive(requestedViewBox.height, "viewBoxHeight");

        int clampedX = Math.clamp(requestedViewBox.x, 0, Math.max(0, resolutionWidth - 1));
        int clampedY = Math.clamp(requestedViewBox.y, 0, Math.max(0, resolutionHeight - 1));
        int clampedWidth = Math.clamp(validatedWidth, MIN_SIZE, resolutionWidth - clampedX);
        int clampedHeight = Math.clamp(validatedHeight, MIN_SIZE, resolutionHeight - clampedY);
        viewBox.setBounds(clampedX, clampedY, clampedWidth, clampedHeight);
    }

    private void clampViewBoxToResolution() {
        setViewBoxInternal(viewBox);
    }

    public static final class Builder {
        private String url = "about:blank";
        private boolean transparent = true;
        private int surfaceWidth = MIN_SIZE;
        private int surfaceHeight = MIN_SIZE;
        private boolean autoResolution = true;
        private int resolutionWidth = MIN_SIZE;
        private int resolutionHeight = MIN_SIZE;
        private Rectangle viewBox;
        private CefClient client;
        private CefRequestContext requestContext;
        private GrapheneRenderer renderer;
        private Object owner;

        private Builder() {
        }

        public Builder url(String url) {
            this.url = Objects.requireNonNull(url, "url");
            return this;
        }

        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }

        public Builder surfaceSize(int width, int height) {
            this.surfaceWidth = requirePositive(width, SURFACE_WIDTH_NAME);
            this.surfaceHeight = requirePositive(height, SURFACE_HEIGHT_NAME);
            return this;
        }

        public Builder resolution(int width, int height) {
            this.autoResolution = false;
            this.resolutionWidth = requirePositive(width, "resolutionWidth");
            this.resolutionHeight = requirePositive(height, "resolutionHeight");
            return this;
        }

        public Builder autoResolution() {
            this.autoResolution = true;
            return this;
        }

        public Builder viewBox(int x, int y, int width, int height) {
            this.viewBox = new Rectangle(x, y, width, height);
            return this;
        }

        public Builder client(CefClient client) {
            this.client = Objects.requireNonNull(client, "client");
            return this;
        }

        public Builder requestContext(CefRequestContext requestContext) {
            this.requestContext = Objects.requireNonNull(requestContext, "requestContext");
            return this;
        }

        public Builder renderer(GrapheneRenderer renderer) {
            this.renderer = Objects.requireNonNull(renderer, "renderer");
            return this;
        }

        public Builder owner(Object owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
            return this;
        }

        public BrowserSurface build() {
            BrowserSurface surface = new BrowserSurface(this);
            if (owner != null) {
                GrapheneCore.surfaces().register(owner, surface);
            }

            return surface;
        }
    }
}
