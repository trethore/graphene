package tytoo.grapheneui.api.surface;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.cef.CefBrowserSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefRequestContext;
import org.cef.network.CefRequest;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.browser.GrapheneInputCaptureState;
import tytoo.grapheneui.internal.browser.surface.BrowserSurfaceLoadListenerScope;
import tytoo.grapheneui.internal.browser.surface.BrowserSurfaceSizingState;
import tytoo.grapheneui.internal.browser.surface.BrowserSurfaceTitleListenerScope;
import tytoo.grapheneui.internal.core.GrapheneCoreServices;
import tytoo.grapheneui.internal.mc.McClient;
import tytoo.grapheneui.internal.mc.McWindowScale;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
    private static final String INPUT_CAPTURE_CHANNEL = "graphene:input:capture";
    private static final String INPUT_RELEASE_CHANNEL = "graphene:input:release";
    private static final String INPUT_MOVE_CHANNEL = "graphene:input:move";
    private static final String INPUT_RELEASED_CHANNEL = "graphene:input:released";
    private static final String INPUT_CAPTURE_CHANGED_CHANNEL = "graphene:input:capture-changed";
    private static final String OWNER_NAME = "owner";
    private static final String CURSOR_NAME = "cursor";
    private static final String ESCAPE_NAME = "escape";
    private static final String SURFACE_WIDTH_NAME = "surfaceWidth";
    private static final String SURFACE_HEIGHT_NAME = "surfaceHeight";
    private static final String NAVIGATION_RELEASE_REASON = "navigation";
    private static final Consumer<CefRequestContext> NO_OP_REQUEST_CONTEXT_CUSTOMIZER = ignoredRequestContext -> {
    };

    private final GrapheneBrowser browser;
    private final GrapheneBridge bridge;
    private final BrowserSurfaceSizingState sizingState;
    private final BrowserSurfaceLoadListenerScope loadListenerScope;
    private final BrowserSurfaceTitleListenerScope titleListenerScope;
    private final GrapheneCoreServices services;
    private final GrapheneInputCaptureState inputCaptureState = new GrapheneInputCaptureState();
    private final GrapheneBridgeSubscription inputCaptureSubscription;
    private final GrapheneBridgeSubscription inputReleaseSubscription;
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

        CefRequestContext requestContext = CefRequestContext.getGlobalContext();
        builder.requestContextCustomizer.accept(requestContext);
        BrowserSurfaceConfig config = builder.config != null ? builder.config : BrowserSurfaceConfig.defaults();

        this.browser = new GrapheneBrowser(
                services.runtimeInternal().requireClient(),
                builder.url,
                builder.transparent,
                requestContext,
                config.toCefBrowserSettings()
        );
        this.bridge = services.runtimeInternal().attachBridge(this.browser);
        this.inputCaptureSubscription = this.bridge.onRequest(INPUT_CAPTURE_CHANNEL, this::onInputCaptureRequested);
        this.inputReleaseSubscription = this.bridge.onRequest(INPUT_RELEASE_CHANNEL, this::onInputReleaseRequested);
        this.browser.createImmediately();
        this.loadListenerScope = new BrowserSurfaceLoadListenerScope(this.browser, services.runtimeInternal().getLoadEventBus());
        this.titleListenerScope = new BrowserSurfaceTitleListenerScope(this.browser, services.runtimeInternal().getTitleEventBus());
        this.loadListenerScope.add(createInputCaptureReleaseLoadListener());
        this.browser.wasResizedTo(sizingState.resolutionWidth(), sizingState.resolutionHeight());
        if (builder.owner != null) {
            services.surfaceManager().register(builder.owner, this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        releaseInputCapture("surface-closed");
        inputCaptureSubscription.unsubscribe();
        inputReleaseSubscription.unsubscribe();
        services.surfaceManager().unregister(this);
        loadListenerScope.close();
        titleListenerScope.close();
        services.runtimeInternal().detachBridge(browser);
        browser.close();
    }

    public void loadUrl(String url) {
        ensureOpen();
        releaseInputCapture(NAVIGATION_RELEASE_REASON);
        services.runtimeInternal().onNavigationRequested(browser);
        browser.loadURL(url);
    }

    public void goBack() {
        ensureOpen();
        releaseInputCapture(NAVIGATION_RELEASE_REASON);
        services.runtimeInternal().onNavigationRequested(browser);
        browser.goBack();
    }

    public void goForward() {
        ensureOpen();
        releaseInputCapture(NAVIGATION_RELEASE_REASON);
        services.runtimeInternal().onNavigationRequested(browser);
        browser.goForward();
    }

    public void reload() {
        ensureOpen();
        releaseInputCapture(NAVIGATION_RELEASE_REASON);
        services.runtimeInternal().onNavigationRequested(browser);
        browser.reload();
    }

    public void setOwner(Object owner) {
        ensureOpen();
        services.surfaceManager().register(Objects.requireNonNull(owner, OWNER_NAME), this);
    }

    public void clearOwner() {
        ensureOpen();
        services.surfaceManager().unregister(this);
    }

    public Subscription subscribeLoadListener(GrapheneLoadListener loadListener) {
        ensureOpen();
        loadListenerScope.add(loadListener);
        return () -> loadListenerScope.remove(loadListener);
    }

    public void addLoadListener(GrapheneLoadListener loadListener) {
        ensureOpen();
        loadListenerScope.add(loadListener);
    }

    public void removeLoadListener(GrapheneLoadListener loadListener) {
        ensureOpen();
        loadListenerScope.remove(loadListener);
    }

    public Subscription subscribeTitleListener(GrapheneTitleListener titleListener) {
        ensureOpen();
        titleListenerScope.add(titleListener);
        return () -> titleListenerScope.remove(titleListener);
    }

    public void addTitleListener(GrapheneTitleListener titleListener) {
        ensureOpen();
        titleListenerScope.add(titleListener);
    }

    public void removeTitleListener(GrapheneTitleListener titleListener) {
        ensureOpen();
        titleListenerScope.remove(titleListener);
    }

    public void setSurfaceSize(int width, int height) {
        ensureOpen();
        BrowserSurfaceSizingState.ResizeInstruction resizeInstruction = sizingState.setSurfaceSize(
                width,
                height,
                McWindowScale.getScaleX(),
                McWindowScale.getScaleY()
        );
        applyResizeInstruction(resizeInstruction);
    }

    public void setResolution(int width, int height) {
        ensureOpen();
        BrowserSurfaceSizingState.ResizeInstruction resizeInstruction = sizingState.setResolution(width, height);
        applyResizeInstruction(resizeInstruction);
    }

    public void useAutoResolution() {
        ensureOpen();
        BrowserSurfaceSizingState.ResizeInstruction resizeInstruction = sizingState.useAutoResolution(
                McWindowScale.getScaleX(),
                McWindowScale.getScaleY()
        );
        applyResizeInstruction(resizeInstruction);
    }

    public void setViewBox(int x, int y, int width, int height) {
        ensureOpen();
        sizingState.setViewBox(x, y, width, height);
    }

    public void resetViewBox() {
        ensureOpen();
        sizingState.resetViewBox();
    }

    public Point toBrowserPoint(double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        ensureOpen();
        return sizingState.toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight);
    }

    public int toBrowserX(double surfaceX, int renderedWidth) {
        ensureOpen();
        return sizingState.toBrowserX(surfaceX, renderedWidth);
    }

    public int toBrowserY(double surfaceY, int renderedHeight) {
        ensureOpen();
        return sizingState.toBrowserY(surfaceY, renderedHeight);
    }

    int toBrowserDeltaX(double surfaceDeltaX, int renderedWidth) {
        ensureOpen();
        return sizingState.toBrowserDeltaX(surfaceDeltaX, renderedWidth);
    }

    int toBrowserDeltaY(double surfaceDeltaY, int renderedHeight) {
        ensureOpen();
        return sizingState.toBrowserDeltaY(surfaceDeltaY, renderedHeight);
    }

    boolean isCursorCaptured() {
        ensureOpen();
        return inputCaptureState.isCursorCaptured();
    }

    boolean shouldConsumeScreenEscape() {
        ensureOpen();
        return switch (inputCaptureState.escapeMode()) {
            case RELEASE, PASSTHROUGH -> true;
            case MINECRAFT -> false;
        };
    }

    boolean shouldReleaseCaptureOnEscape() {
        ensureOpen();
        return inputCaptureState.escapeMode() == GrapheneInputCaptureState.EscapeMode.RELEASE;
    }

    void releaseInputCapture(String reason) {
        if (!inputCaptureState.isActive()) {
            return;
        }

        boolean previousCursorCaptured = inputCaptureState.isCursorCaptured();
        inputCaptureState.release();
        if (previousCursorCaptured) {
            McClient.setCursorDisabled(false);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("reason", reason == null ? "unknown" : reason);
        emitBridgeEvent(INPUT_RELEASED_CHANNEL, payload.toString());
        emitInputCaptureChanged();
    }

    void emitInputMove(int movementX, int movementY) {
        ensureOpen();
        if (!inputCaptureState.isCursorCaptured() || (movementX == 0 && movementY == 0)) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("movementX", movementX);
        payload.addProperty("movementY", movementY);
        emitBridgeEvent(INPUT_MOVE_CHANNEL, payload.toString());
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        ensureOpen();
        render(guiGraphics, x, y, sizingState.surfaceWidth(), sizingState.surfaceHeight());
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        ensureOpen();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("graphene");
        try {
            pushBootstrap(profiler);
            pushRender(profiler, guiGraphics, x, y, width, height);
        } finally {
            profiler.pop();
        }
    }

    GrapheneBrowser internalBrowser() {
        return browser;
    }

    private void applyResizeInstruction(BrowserSurfaceSizingState.ResizeInstruction resizeInstruction) {
        if (!resizeInstruction.shouldResizeBrowser()) {
            return;
        }

        browser.wasResizedTo(resizeInstruction.width(), resizeInstruction.height());
    }

    private GrapheneLoadListener createInputCaptureReleaseLoadListener() {
        return new GrapheneLoadListener() {
            @Override
            public void onLoadStart(CefBrowser ignoredBrowser, CefFrame frame, CefRequest.TransitionType transitionType) {
                if (frame == null || frame.isMain()) {
                    releaseInputCapture(NAVIGATION_RELEASE_REASON);
                }
            }
        };
    }

    private CompletableFuture<String> onInputCaptureRequested(String ignoredChannel, String payloadJson) {
        JsonObject payload = parseObject(payloadJson);
        boolean cursorCaptured = readCursorCaptured(payload);
        GrapheneInputCaptureState.EscapeMode escapeMode = readEscapeMode(payload);
        boolean previousCursorCaptured = inputCaptureState.isCursorCaptured();
        inputCaptureState.capture(cursorCaptured, escapeMode);
        if (previousCursorCaptured != cursorCaptured) {
            McClient.setCursorDisabled(cursorCaptured);
        }
        emitInputCaptureChanged();
        return CompletableFuture.completedFuture(inputCaptureStateJson());
    }

    private CompletableFuture<String> onInputReleaseRequested(String ignoredChannel, String payloadJson) {
        JsonObject payload = parseObject(payloadJson);
        releaseInputCapture(readString(payload, "reason", "api"));
        return CompletableFuture.completedFuture(inputCaptureStateJson());
    }

    private GrapheneInputCaptureState.EscapeMode readEscapeMode(JsonObject payload) {
        if (!payload.has(ESCAPE_NAME)) {
            return GrapheneInputCaptureState.EscapeMode.MINECRAFT;
        }

        if (payload.get(ESCAPE_NAME).isJsonPrimitive() && payload.get(ESCAPE_NAME).getAsJsonPrimitive().isBoolean()) {
            return payload.get(ESCAPE_NAME).getAsBoolean()
                    ? GrapheneInputCaptureState.EscapeMode.RELEASE
                    : GrapheneInputCaptureState.EscapeMode.MINECRAFT;
        }

        return switch (readString(payload, ESCAPE_NAME, "minecraft")) {
            case "browser-first", "release" -> GrapheneInputCaptureState.EscapeMode.RELEASE;
            case "passthrough" -> GrapheneInputCaptureState.EscapeMode.PASSTHROUGH;
            default -> GrapheneInputCaptureState.EscapeMode.MINECRAFT;
        };
    }

    private JsonObject parseObject(String payloadJson) {
        try {
            return JsonParser.parseString(payloadJson).getAsJsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    private boolean readCursorCaptured(JsonObject payload) {
        if (!payload.has(CURSOR_NAME) || !payload.get(CURSOR_NAME).isJsonPrimitive()) {
            return false;
        }

        try {
            return payload.get(CURSOR_NAME).getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String readString(JsonObject payload, String name, String defaultValue) {
        return payload.has(name) && payload.get(name).isJsonPrimitive()
                ? payload.get(name).getAsString()
                : defaultValue;
    }

    private void emitInputCaptureChanged() {
        emitBridgeEvent(INPUT_CAPTURE_CHANGED_CHANNEL, inputCaptureStateJson());
    }

    private String inputCaptureStateJson() {
        JsonObject payload = new JsonObject();
        payload.addProperty("active", inputCaptureState.isActive());
        payload.addProperty(CURSOR_NAME, inputCaptureState.isCursorCaptured());
        payload.addProperty(ESCAPE_NAME, escapeModeName());
        return payload.toString();
    }

    private String escapeModeName() {
        return switch (inputCaptureState.escapeMode()) {
            case RELEASE -> "release";
            case PASSTHROUGH -> "passthrough";
            case MINECRAFT -> "minecraft";
        };
    }

    private void emitBridgeEvent(String channel, String payloadJson) {
        try {
            bridge.emit(channel, payloadJson);
        } catch (IllegalStateException ignored) {
            // Ignore events while the bridge is shutting down.
        }
    }

    private void pushBootstrap(ProfilerFiller profiler) {
        profiler.push("bootstrap");
        try {
            services.runtimeInternal().ensureBootstrap(browser);
        } finally {
            profiler.pop();
        }
    }

    private void pushRender(ProfilerFiller profiler, GuiGraphics guiGraphics, int x, int y, int width, int height) {
        profiler.push("render");
        try {
            browser.render(
                    guiGraphics,
                    x,
                    y,
                    width,
                    height,
                    sizingState.viewBoxX(),
                    sizingState.viewBoxY(),
                    sizingState.viewBoxWidth(),
                    sizingState.viewBoxHeight()
            );
        } finally {
            profiler.pop();
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("BrowserSurface is closed");
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public GrapheneBridge bridge() {
        ensureOpen();
        return bridge;
    }

    public boolean canGoBack() {
        ensureOpen();
        return browser.canGoBack();
    }

    public boolean canGoForward() {
        ensureOpen();
        return browser.canGoForward();
    }

    public boolean isLoading() {
        ensureOpen();
        return browser.isLoading();
    }

    public CursorType getRequestedCursor() {
        ensureOpen();
        return browser.getRequestedCursor();
    }

    public String currentUrl() {
        ensureOpen();
        return browser.currentUrl();
    }

    public String currentTitle() {
        ensureOpen();
        return browser.currentTitle();
    }

    public int getSurfaceWidth() {
        ensureOpen();
        return sizingState.surfaceWidth();
    }

    public int getSurfaceHeight() {
        ensureOpen();
        return sizingState.surfaceHeight();
    }

    public int getResolutionWidth() {
        ensureOpen();
        return sizingState.resolutionWidth();
    }

    public int getResolutionHeight() {
        ensureOpen();
        return sizingState.resolutionHeight();
    }

    public Rectangle getViewBox() {
        ensureOpen();
        return sizingState.viewBox();
    }

    public boolean isAutoResolution() {
        ensureOpen();
        return sizingState.isAutoResolution();
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
        private Consumer<CefRequestContext> requestContextCustomizer = NO_OP_REQUEST_CONTEXT_CUSTOMIZER;
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

        public Builder requestContextCustomizer(Consumer<CefRequestContext> requestContextCustomizer) {
            this.requestContextCustomizer = this.requestContextCustomizer.andThen(
                    Objects.requireNonNull(requestContextCustomizer, "requestContextCustomizer")
            );
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
