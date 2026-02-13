package tytoo.grapheneui.internal.browser;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserNAccessor;
import org.cef.browser.CefPaintEvent;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.input.CefKeyEvent;
import org.cef.input.CefMouseEvent;
import org.cef.input.CefMouseWheelEvent;
import tytoo.grapheneui.api.render.GrapheneRenderTarget;
import tytoo.grapheneui.api.render.GrapheneRenderer;
import tytoo.grapheneui.internal.render.GrapheneGuiRenderTarget;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class GrapheneBrowser extends CefBrowserNAccessor implements CefRenderHandler, AutoCloseable {
    private final long windowHandle = this.hashCode();
    private final GrapheneRenderer renderer;
    private final boolean transparent;
    private final GrapheneInputBridge inputBridge = new GrapheneInputBridge();
    private final GraphenePaintBuffer paintBuffer = new GraphenePaintBuffer();
    private final GrapheneFocusUtil focusUtil = new GrapheneFocusUtil(this::setNativeFocus);
    private final Rectangle browserRect = new Rectangle(0, 0, 1, 1);
    private final Point screenPoint = new Point(0, 0);
    private volatile int cursorType = Cursor.DEFAULT_CURSOR;
    private boolean closed = false;


    @SuppressWarnings("unused") // Util constructor for simple browser creation with default settings.
    public GrapheneBrowser(CefClient client, String url, boolean transparent, CefRequestContext context, GrapheneRenderer renderer) {
        this(client, url, transparent, context, renderer, new CefBrowserSettings());
    }

    public GrapheneBrowser(
            CefClient client,
            String url,
            boolean transparent,
            CefRequestContext context,
            GrapheneRenderer renderer,
            CefBrowserSettings browserSettings
    ) {
        this(client, url, transparent, context, renderer, browserSettings, null, null);
    }

    private GrapheneBrowser(
            CefClient client,
            String url,
            boolean transparent,
            CefRequestContext context,
            GrapheneRenderer renderer,
            CefBrowserSettings browserSettings,
            CefBrowserNAccessor parent,
            Point inspectAt
    ) {
        super(client, url, context, parent, inspectAt, Objects.requireNonNull(browserSettings, "browserSettings"));
        this.transparent = transparent;
        this.renderer = renderer;
    }

    @Override
    public void createImmediately() {
        setCloseAllowed();
        createBrowserIfRequired();
    }

    @Override
    public synchronized void onBeforeClose() {
        super.onBeforeClose();
        renderer.destroy();
    }

    @Override
    public Component getUIComponent() {
        return inputBridge.uiComponent();
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return browserRect;
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        screenInfo.Set(1.0, 32, 8, false, browserRect.getBounds(), browserRect.getBounds());
        return true;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point point = new Point(screenPoint);
        point.translate(viewPoint.x, viewPoint.y);
        return point;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (!show) {
            renderer.onPopupClosed();
            invalidate();
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        renderer.onPopupSize(size);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        paintBuffer.capture(popup, dirtyRects, buffer, width, height);
    }

    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
        // Not used by this browser implementation because rendering is pulled via updateRendererFrame().
    }

    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
        // Not used by this browser implementation because rendering is pulled via updateRendererFrame().
    }

    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
        // Not used by this browser implementation because rendering is pulled via updateRendererFrame().
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        this.cursorType = cursorType;
        return true;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return true;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
        // No-op: Minecraft cursor handling is managed outside CEF drag operations.
    }

    // Note: in graphene we don't use nativeResolution
    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return renderer.createScreenshot();
    }

    @Override
    public void close() {
        this.close(true);
    }

    @Override
    public void close(boolean force) {
        if (closed) {
            return;
        }

        closed = true;
        renderer.destroy();
        super.close(force);
    }

    @Override
    public void setFocus(boolean enable) {
        focusUtil.setFocused(enable);
    }

    @Override
    protected CefBrowserNAccessor createDevToolsBrowserAccessor(
            CefClient client,
            String url,
            CefRequestContext context,
            CefBrowserNAccessor parent,
            Point inspectAt
    ) {
        return new GrapheneBrowser(
                client,
                url == null ? "about:blank" : url,
                false,
                context,
                new NoopRenderer(),
                new CefBrowserSettings(),
                parent,
                inspectAt
        );
    }

    public void updateRendererFrame() {
        paintBuffer.flushTo(renderer);
    }

    public void renderTo(int x, int y, int width, int height, GuiGraphics guiGraphics) {
        renderer.render(GrapheneGuiRenderTarget.of(guiGraphics), x, y, width, height);
    }

    public void renderTo(int x, int y, int width, int height, GrapheneRenderTarget renderTarget) {
        renderer.render(renderTarget, x, y, width, height);
    }

    public void renderTo(
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            GuiGraphics guiGraphics
    ) {
        renderer.renderRegion(
                GrapheneGuiRenderTarget.of(guiGraphics),
                x,
                y,
                width,
                height,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight
        );
    }

    public void renderTo(
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            GrapheneRenderTarget renderTarget
    ) {
        renderer.renderRegion(renderTarget, x, y, width, height, sourceX, sourceY, sourceWidth, sourceHeight);
    }

    public void wasResizedTo(int width, int height) {
        browserRect.setBounds(0, 0, width, height);
        super.wasResized(width, height);
    }

    public void mouseMoved(int x, int y, int modifiers) {
        inputBridge.mouseMoved(this, x, y, modifiers);
    }

    public void mouseDragged(double x, double y, int button) {
        inputBridge.mouseDragged(this, x, y, button);
    }

    public void mouseInteracted(int x, int y, int modifiers, int button, boolean pressed, int clickCount) {
        inputBridge.mouseInteracted(this, x, y, modifiers, button, pressed, clickCount);
    }

    public void mouseScrolled(int x, int y, int modifiers, int amount, int rotation) {
        inputBridge.mouseScrolled(this, x, y, modifiers, amount, rotation);
    }

    public void keyTyped(char character, int modifiers) {
        inputBridge.keyTyped(this, character, modifiers);
    }

    public void keyEventByKeyCode(int keyCode, int scanCode, int modifiers, boolean pressed) {
        inputBridge.keyEventByKeyCode(this, keyCode, scanCode, modifiers, pressed);
    }

    void dispatchMouseEvent(CefMouseEvent event) {
        sendCefMouseEvent(event);
    }

    void dispatchMouseWheelEvent(CefMouseWheelEvent event) {
        sendCefMouseWheelEvent(event);
    }

    void dispatchCefKeyEvent(CefKeyEvent event) {
        sendCefKeyEvent(event);
    }

    public void onTitleChange(String title) {
        renderer.onTitleChange(title);
    }

    public CursorType getRequestedCursor() {
        return switch (cursorType) {
            case Cursor.CROSSHAIR_CURSOR -> CursorTypes.CROSSHAIR;
            case Cursor.TEXT_CURSOR -> CursorTypes.IBEAM;
            case Cursor.HAND_CURSOR -> CursorTypes.POINTING_HAND;
            case Cursor.N_RESIZE_CURSOR,
                 Cursor.S_RESIZE_CURSOR -> CursorTypes.RESIZE_NS;
            case Cursor.E_RESIZE_CURSOR,
                 Cursor.W_RESIZE_CURSOR -> CursorTypes.RESIZE_EW;
            case Cursor.NE_RESIZE_CURSOR,
                 Cursor.NW_RESIZE_CURSOR,
                 Cursor.SE_RESIZE_CURSOR,
                 Cursor.SW_RESIZE_CURSOR,
                 Cursor.MOVE_CURSOR -> CursorTypes.RESIZE_ALL;
            default -> CursorTypes.ARROW;
        };
    }

    public void executeScript(String script) {
        Objects.requireNonNull(script, "script");
        executeJavaScript(script, currentUrl(), 0);
    }

    public void executeScript(String script, String url) {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(url, "url");
        executeJavaScript(script, url, 0);
    }

    public String currentUrl() {
        String currentUrl = getURL();
        if (currentUrl == null || currentUrl.isBlank()) {
            return "about:blank";
        }

        return currentUrl;
    }

    private void setNativeFocus(boolean enable) {
        super.setFocus(enable);
    }

    private void createBrowserIfRequired() {
        if (getNativeRef("CefBrowser") != 0L) {
            return;
        }

        if (getParentBrowser() == null) {
            createBrowser(getClient(), windowHandle, getUrl(), true, transparent, null, getRequestContext());
        }
    }

    private static final class NoopRenderer implements GrapheneRenderer {
        @Override
        public void render(GrapheneRenderTarget renderTarget, int x, int y, int width, int height) {
            // Intentionally empty: DevTools helper browser is not rendered in-game.
        }

        @Override
        public void destroy() {
            // Intentionally empty: no native resources are allocated by this renderer.
        }

        @Override
        public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender) {
            // Intentionally empty: DevTools helper browser does not consume paint frames.
        }

        @Override
        public void onPopupSize(Rectangle rect) {
            // Intentionally empty: popup state is irrelevant for the DevTools helper browser.
        }

        @Override
        public void onPopupClosed() {
            // Intentionally empty: popup state is irrelevant for the DevTools helper browser.
        }

        @Override
        public CompletableFuture<BufferedImage> createScreenshot() {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Noop renderer"));
        }
    }
}
