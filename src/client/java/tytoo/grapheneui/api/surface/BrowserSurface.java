package tytoo.grapheneui.api.surface;

import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.gui.GuiGraphics;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefRequestContext;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.render.GrapheneRenderTarget;
import tytoo.grapheneui.api.render.GrapheneRenderer;
import tytoo.grapheneui.internal.browser.BrowserSurfaceLoadListenerScope;
import tytoo.grapheneui.internal.browser.BrowserSurfaceSizingState;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.core.GrapheneCoreServices;
import tytoo.grapheneui.internal.mc.McWindowScale;
import tytoo.grapheneui.internal.render.GrapheneLwjglRenderer;

import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a browser surface that can be rendered onto Minecraft's GUI.
 * <p>
 * The surface size and resolution can be configured independently, allowing for flexible rendering options.
 * The surface can also have an optional view box to control the portion of the browser content that is rendered.
 * <p>
 * The browser surface provides methods for navigation, loading URLs, and subscribing to load events.
 * It also manages its own lifecycle and should be closed when no longer needed to free resources.
 */

@SuppressWarnings("unused") // Public API
public final class BrowserSurface implements AutoCloseable {
    private static final int MIN_SIZE = 1;
    private static final String OWNER_NAME = "owner";
    private static final String SURFACE_WIDTH_NAME = "surfaceWidth";
    private static final String SURFACE_HEIGHT_NAME = "surfaceHeight";
    private static final Consumer<CefRequestContext> NO_OP_REQUEST_CONTEXT_CUSTOMIZER = ignoredRequestContext -> {
    };

    private final GrapheneBrowser browser;
    private final GrapheneBridge bridge;
    private final BrowserSurfaceSizingState sizingState;
    private final BrowserSurfaceLoadListenerScope loadListenerScope;
    private final GrapheneCoreServices services;
    private boolean closed;

    private BrowserSurface(Builder builder) {
        this.services = GrapheneCoreServices.get();
        this.sizingState = new BrowserSurfaceSizingState(
                builder.surfaceWidth,
                builder.surfaceHeight,
                builder.autoResolution,
                builder.resolutionWidth,
                builder.resolutionHeight,
                builder.viewBox,
                McWindowScale.getScaleX(),
                McWindowScale.getScaleY()
        );

        GrapheneCore.runtime();

        CefClient cefClient = builder.client != null ? builder.client : services.runtimeInternal().requireClient();
        CefRequestContext requestContext = builder.requestContext != null ? builder.requestContext : CefRequestContext.getGlobalContext();
        builder.requestContextCustomizer.accept(requestContext);
        GrapheneRenderer renderer = builder.renderer != null ? builder.renderer : new GrapheneLwjglRenderer(builder.transparent);
        BrowserSurfaceConfig config = builder.config != null ? builder.config : BrowserSurfaceConfig.defaults();

        this.browser = new GrapheneBrowser(
                cefClient,
                builder.url,
                builder.transparent,
                requestContext,
                renderer,
                config.toCefBrowserSettings()
        );
        this.bridge = services.runtimeInternal().attachBridge(this.browser);
        this.browser.createImmediately();
        this.loadListenerScope = new BrowserSurfaceLoadListenerScope(this.browser, services.runtimeInternal().getLoadEventBus());
        this.browser.wasResizedTo(sizingState.resolutionWidth(), sizingState.resolutionHeight());
        if (builder.owner != null) {
            services.surfaceManager().register(builder.owner, this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    GrapheneBrowser internalBrowser() {
        return browser;
    }

    public GrapheneBridge bridge() {
        return bridge;
    }

    public boolean canGoBack() {
        return browser.canGoBack();
    }

    public boolean canGoForward() {
        return browser.canGoForward();
    }

    public boolean isLoading() {
        return browser.isLoading();
    }

    public CursorType getRequestedCursor() {
        return browser.getRequestedCursor();
    }

    public String currentUrl() {
        return browser.currentUrl();
    }

    public void loadUrl(String url) {
        services.runtimeInternal().onNavigationRequested(browser);
        browser.loadURL(url);
    }

    public void goBack() {
        services.runtimeInternal().onNavigationRequested(browser);
        browser.goBack();
    }

    public void goForward() {
        services.runtimeInternal().onNavigationRequested(browser);
        browser.goForward();
    }

    public void reload() {
        services.runtimeInternal().onNavigationRequested(browser);
        browser.reload();
    }

    public int getSurfaceWidth() {
        return sizingState.surfaceWidth();
    }

    public int getSurfaceHeight() {
        return sizingState.surfaceHeight();
    }

    public int getResolutionWidth() {
        return sizingState.resolutionWidth();
    }

    public int getResolutionHeight() {
        return sizingState.resolutionHeight();
    }

    public Rectangle getViewBox() {
        return sizingState.viewBox();
    }

    public boolean isAutoResolution() {
        return sizingState.isAutoResolution();
    }

    public void setOwner(Object owner) {
        ensureOpen();
        services.surfaceManager().register(Objects.requireNonNull(owner, OWNER_NAME), this);
    }

    public void clearOwner() {
        services.surfaceManager().unregister(this);
    }

    public Subscription subscribeLoadListener(GrapheneLoadListener loadListener) {
        return loadListenerScope.subscribe(loadListener);
    }

    public void addLoadListener(GrapheneLoadListener loadListener) {
        loadListenerScope.add(loadListener);
    }

    public void removeLoadListener(GrapheneLoadListener loadListener) {
        loadListenerScope.remove(loadListener);
    }

    public void setSurfaceSize(int width, int height) {
        BrowserSurfaceSizingState.ResizeInstruction resizeInstruction = sizingState.setSurfaceSize(
                width,
                height,
                McWindowScale.getScaleX(),
                McWindowScale.getScaleY()
        );
        applyResizeInstruction(resizeInstruction);
    }

    public void setResolution(int width, int height) {
        BrowserSurfaceSizingState.ResizeInstruction resizeInstruction = sizingState.setResolution(width, height);
        applyResizeInstruction(resizeInstruction);
    }

    public void useAutoResolution() {
        BrowserSurfaceSizingState.ResizeInstruction resizeInstruction = sizingState.useAutoResolution(
                McWindowScale.getScaleX(),
                McWindowScale.getScaleY()
        );
        applyResizeInstruction(resizeInstruction);
    }

    public void setViewBox(int x, int y, int width, int height) {
        sizingState.setViewBox(x, y, width, height);
    }

    public void resetViewBox() {
        sizingState.resetViewBox();
    }

    public Point toBrowserPoint(double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        return sizingState.toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight);
    }

    public int toBrowserX(double surfaceX, int renderedWidth) {
        return sizingState.toBrowserX(surfaceX, renderedWidth);
    }

    public int toBrowserY(double surfaceY, int renderedHeight) {
        return sizingState.toBrowserY(surfaceY, renderedHeight);
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        render(guiGraphics, x, y, sizingState.surfaceWidth(), sizingState.surfaceHeight());
    }

    public void render(GrapheneRenderTarget renderTarget, int x, int y) {
        render(renderTarget, x, y, sizingState.surfaceWidth(), sizingState.surfaceHeight());
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        services.runtimeInternal().ensureBootstrap(browser);
        browser.updateRendererFrame();
        browser.renderTo(
                x,
                y,
                width,
                height,
                sizingState.viewBoxX(),
                sizingState.viewBoxY(),
                sizingState.viewBoxWidth(),
                sizingState.viewBoxHeight(),
                guiGraphics
        );
    }

    public void render(GrapheneRenderTarget renderTarget, int x, int y, int width, int height) {
        services.runtimeInternal().ensureBootstrap(browser);
        browser.updateRendererFrame();
        browser.renderTo(
                x,
                y,
                width,
                height,
                sizingState.viewBoxX(),
                sizingState.viewBoxY(),
                sizingState.viewBoxWidth(),
                sizingState.viewBoxHeight(),
                renderTarget
        );
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        services.surfaceManager().unregister(this);
        loadListenerScope.close();
        services.runtimeInternal().detachBridge(browser);
        browser.close();
    }

    private void applyResizeInstruction(BrowserSurfaceSizingState.ResizeInstruction resizeInstruction) {
        if (!resizeInstruction.shouldResizeBrowser()) {
            return;
        }

        browser.wasResizedTo(resizeInstruction.width(), resizeInstruction.height());
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("BrowserSurface is closed");
        }
    }

    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        void unsubscribe();

        @Override
        default void close() {
            unsubscribe();
        }
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
        private Consumer<CefRequestContext> requestContextCustomizer = NO_OP_REQUEST_CONTEXT_CUSTOMIZER;
        private GrapheneRenderer renderer;
        private BrowserSurfaceConfig config = BrowserSurfaceConfig.defaults();
        private Object owner;

        private Builder() {
        }

        private static int requirePositive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0");
            }

            return value;
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

        public Builder requestContextCustomizer(Consumer<CefRequestContext> requestContextCustomizer) {
            this.requestContextCustomizer = this.requestContextCustomizer.andThen(
                    Objects.requireNonNull(requestContextCustomizer, "requestContextCustomizer")
            );
            return this;
        }

        public Builder renderer(GrapheneRenderer renderer) {
            this.renderer = Objects.requireNonNull(renderer, "renderer");
            return this;
        }

        public Builder owner(Object owner) {
            this.owner = Objects.requireNonNull(owner, OWNER_NAME);
            return this;
        }

        public Builder config(BrowserSurfaceConfig config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        public Builder maxFps(int maxFps) {
            this.config = this.config.withMaxFps(maxFps);
            return this;
        }

        public Builder settingsCustomizer(Consumer<CefBrowserSettings> settingsCustomizer) {
            this.config = this.config.withSettingsCustomizer(settingsCustomizer);
            return this;
        }

        public BrowserSurface build() {
            return new BrowserSurface(this);
        }
    }
}
