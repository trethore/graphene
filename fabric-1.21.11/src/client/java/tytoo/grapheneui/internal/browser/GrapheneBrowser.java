package tytoo.grapheneui.internal.browser;

import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.gui.GuiGraphics;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWindowless;
import org.cef.browser.CefPaintEvent;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.input.CefMouseEvent;
import org.cef.input.CefMouseWheelEvent;
import tytoo.grapheneui.internal.browser.cef.GrapheneBrowserFocusGuard;
import tytoo.grapheneui.internal.browser.cef.GrapheneCefRenderAdapter;
import tytoo.grapheneui.internal.browser.drag.GrapheneDragSession;
import tytoo.grapheneui.internal.browser.input.GrapheneCefMouseTarget;
import tytoo.grapheneui.internal.browser.input.GrapheneInputBridge;
import tytoo.grapheneui.internal.browser.input.GrapheneMouseInputTarget;
import tytoo.grapheneui.internal.browser.input.devtools.GrapheneDevToolsMethodExecutor;
import tytoo.grapheneui.internal.browser.input.devtools.GrapheneDevToolsTarget;
import tytoo.grapheneui.internal.browser.input.devtools.GrapheneDomKeyboardDispatcher;
import tytoo.grapheneui.internal.browser.input.devtools.GrapheneDomMouseDispatcher;
import tytoo.grapheneui.internal.browser.render.GrapheneBrowserGpuRenderer;
import tytoo.grapheneui.internal.browser.render.GraphenePaintBuffer;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeBrowser;
import tytoo.grapheneui.internal.browser.title.GrapheneBrowserTitleState;
import tytoo.grapheneui.internal.cef.GrapheneTitleTarget;
import tytoo.grapheneui.internal.mc.McClient;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class GrapheneBrowser extends CefBrowserWindowless implements CefRenderHandler, GrapheneBridgeBrowser, GrapheneDevToolsTarget, GrapheneTitleTarget, GrapheneCefMouseTarget, GrapheneMouseInputTarget, AutoCloseable {
    private final GrapheneBrowserGpuRenderer renderer;
    private final boolean transparent;
    private final GrapheneInputBridge inputBridge = new GrapheneInputBridge();
    private final GrapheneDomMouseDispatcher mouseDispatcher;
    private final GrapheneDomKeyboardDispatcher keyboardDispatcher;
    private final GraphenePaintBuffer paintBuffer = new GraphenePaintBuffer();
    private final GrapheneCefRenderAdapter renderAdapter = new GrapheneCefRenderAdapter(paintBuffer, this::invalidate);
    private final GrapheneBrowserFocusGuard focusGuard = new GrapheneBrowserFocusGuard();
    private final GrapheneBrowserTitleState titleState = new GrapheneBrowserTitleState();
    private final GrapheneDragSession dragSession;
    private boolean closed = false;


    @SuppressWarnings("unused") // Util constructor for simple browser creation with default settings.
    public GrapheneBrowser(CefClient client, String url, boolean transparent, CefRequestContext context) {
        this(client, url, transparent, context, new CefBrowserSettings());
    }

    public GrapheneBrowser(
            CefClient client,
            String url,
            boolean transparent,
            CefRequestContext context,
            CefBrowserSettings browserSettings
    ) {
        this(client, url, transparent, context, browserSettings, null, null);
    }

    private GrapheneBrowser(
            CefClient client,
            String url,
            boolean transparent,
            CefRequestContext context,
            CefBrowserSettings browserSettings,
            CefBrowserWindowless parent,
            Point inspectAt
    ) {
        super(client, url, context, parent, inspectAt, Objects.requireNonNull(browserSettings, "browserSettings"));
        this.transparent = transparent;
        this.renderer = new GrapheneBrowserGpuRenderer(transparent);
        GrapheneDevToolsMethodExecutor devToolsMethodExecutor = new GrapheneDevToolsMethodExecutor(this);
        this.mouseDispatcher = new GrapheneDomMouseDispatcher(
                (method, payload) -> devToolsMethodExecutor.executeMethod(method, payload, GrapheneDomMouseDispatcher.logger())
        );
        this.keyboardDispatcher = new GrapheneDomKeyboardDispatcher(
                (method, payload) -> devToolsMethodExecutor.executeMethod(method, payload, GrapheneDomKeyboardDispatcher.logger()),
                McClient::nativeWindowHandle
        );
        this.dragSession = new GrapheneDragSession(createDragCallbacks());
    }

    @Override
    public void createImmediately() {
        setCloseAllowed();
        createBrowserIfRequired();
    }

    @Override
    public synchronized void onBeforeClose() {
        super.onBeforeClose();
        renderer.close();
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
        return renderAdapter.getViewRect();
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        return renderAdapter.getScreenInfo(screenInfo);
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        return renderAdapter.getScreenPoint(viewPoint);
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        renderAdapter.onPopupShow(show);
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        renderAdapter.onPopupSize(size);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        renderAdapter.onPaint(popup, dirtyRects, buffer, width, height);
    }

    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> ignoredListener) {
        // Graphene consumes paint frames directly in onPaint() and does not expose a secondary listener pipeline here.
    }

    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> ignoredListener) {
        // Graphene consumes paint frames directly in onPaint() and does not replace that flow with listener-based rendering.
    }

    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> ignoredListener) {
        // No listener state is stored because Graphene uses direct frame capture in onPaint().
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        return renderAdapter.onCursorChange(cursorType);
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return dragSession.start(dragData, mask);
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
        // No-op: Minecraft cursor handling is managed outside CEF drag operations.
    }

    // Note: in graphene we don't use nativeResolution
    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean ignoredNativeResolution) {
        return renderer.createScreenshot(paintBuffer.snapshot());
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

        cancelActiveDrag();
        closed = true;
        renderer.close();
        super.close(force);
    }

    @Override
    protected CefBrowserWindowless createDevToolsBrowserWindowless(
            CefClient client,
            String url,
            CefRequestContext context,
            CefBrowserWindowless parent,
            Point inspectAt
    ) {
        return new GrapheneBrowser(
                client,
                url == null ? "about:blank" : url,
                false,
                context,
                new CefBrowserSettings(),
                parent,
                inspectAt
        );
    }

    public void render(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        renderer.render(
                guiGraphics,
                paintBuffer.snapshot(),
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

    public void wasResizedTo(int width, int height) {
        renderAdapter.resize(width, height);
        super.wasResized(width, height);
    }

    @Override
    public void setFocus(boolean enable) {
        focusGuard.apply(enable, super::setFocus);
    }

    public void mouseMoved(int x, int y, int modifiers) {
        inputBridge.mouseMoved(this, x, y, modifiers);
    }

    public void mouseDragged(double x, double y, int button) {
        inputBridge.mouseDragged(this, x, y, button);
    }

    public void dragUpdated(int x, int y, int modifiers) {
        dragSession.update(x, y, modifiers);
    }

    public void dragCompleted(int x, int y, int modifiers) {
        dragSession.complete(x, y, modifiers);
    }

    public void cancelActiveDrag() {
        dragSession.cancel();
    }

    public void mouseInteracted(int x, int y, int modifiers, int button, boolean pressed, int clickCount) {
        inputBridge.mouseInteracted(this, x, y, modifiers, button, pressed, clickCount);
    }

    public void navigationButtonInteracted(int x, int y, int modifiers, int button, boolean pressed, int clickCount, int buttons) {
        mouseDispatcher.navigationButtonInteracted(x, y, modifiers, button, pressed, clickCount, buttons);
    }

    public void mouseScrolled(int x, int y, int modifiers, int amount, int rotation) {
        inputBridge.mouseScrolled(this, x, y, modifiers, amount, rotation);
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        keyboardDispatcher.keyPressed(keyCode, scanCode, modifiers);
    }

    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        keyboardDispatcher.keyReleased(keyCode, scanCode, modifiers);
    }

    public void textInput(String text) {
        keyboardDispatcher.textInput(text);
    }

    public void resetKeyboardState() {
        keyboardDispatcher.resetState();
    }

    public void dispatchMouseEvent(CefMouseEvent event) {
        sendCefMouseEvent(event);
    }

    public void dispatchMouseWheelEvent(CefMouseWheelEvent event) {
        sendCefMouseWheelEvent(event);
    }

    public CursorType getRequestedCursor() {
        return renderAdapter.requestedCursor();
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

    public String currentTitle() {
        return titleState.currentTitle();
    }

    @Override
    public CefBrowser browser() {
        return this;
    }

    public boolean updateTitle(String title) {
        return titleState.updateTitle(title);
    }

    private void createBrowserIfRequired() {
        if (getNativeRef("CefBrowser") != 0L) {
            return;
        }

        if (getParentBrowser() == null) {
            createBrowser(getClient(), McClient.nativeWindowHandle(), getUrl(), true, transparent, null, getRequestContext());
        }
    }

    private GrapheneDragSession.DragCallbacks createDragCallbacks() {
        return new GrapheneDragSession.DragCallbacks() {
            @Override
            public void enter(CefDragData dragData, Point point, int modifiers, int operationMask) {
                dragTargetDragEnter(dragData, point, modifiers, operationMask);
            }

            @Override
            public void over(Point point, int modifiers, int operationMask) {
                dragTargetDragOver(point, modifiers, operationMask);
            }

            @Override
            public void drop(Point point, int modifiers) {
                dragTargetDrop(point, modifiers);
            }

            @Override
            public void leave() {
                dragTargetDragLeave();
            }

            @Override
            public void sourceEndedAt(Point point, int operation) {
                dragSourceEndedAt(point, operation);
            }

            @Override
            public void systemDragEnded() {
                dragSourceSystemDragEnded();
            }
        };
    }

}
