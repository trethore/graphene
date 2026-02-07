package tytoo.grapheneui.browser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.cef.GrapheneCefRuntime;
import tytoo.grapheneui.event.GrapheneLoadListener;
import tytoo.grapheneui.screen.GrapheneScreenBridge;

import java.awt.*;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GrapheneWebViewWidget extends AbstractWidget implements Closeable {
    private static final String DEFAULT_URL = "classpath://assets/graphene-ui/graphene_test/welcome.html";

    private final Screen screen;
    private final BrowserSurface surface;
    private final GrapheneBrowser browser;
    private final Map<GrapheneLoadListener, GrapheneLoadListener> loadListenerWrappers = new HashMap<>();
    private int lastBrowserMouseX = Integer.MIN_VALUE;
    private int lastBrowserMouseY = Integer.MIN_VALUE;
    private boolean pointerButtonDown = false;

    @SuppressWarnings("unused")
    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message) {
        this(screen, x, y, width, height, message, DEFAULT_URL);
    }

    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message, String url) {
        this(
                screen,
                x,
                y,
                width,
                height,
                message,
                BrowserSurface.builder()
                        .url(url)
                        .surfaceSize(width, height)
                        .build()
        );
    }

    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message, BrowserSurface surface) {
        super(x, y, width, height, message);
        this.screen = screen;
        this.surface = Objects.requireNonNull(surface, "surface");
        this.browser = this.surface.browser();

        if (!(screen instanceof GrapheneScreenBridge screenBridge)) {
            throw new IllegalStateException("Screen does not implement GrapheneScreenBridge: " + screen.getClass().getName());
        }

        screenBridge.grapheneui$addWebViewWidget(this);
        GrapheneCore.surfaces().register(this, this.surface);
        browser.setFocus(true);
        this.surface.setSurfaceSize(width, height);
    }

    public GrapheneBrowser getBrowser() {
        return browser;
    }

    public BrowserSurface getSurface() {
        return surface;
    }

    @SuppressWarnings("unused")
    public void addLoadListener(GrapheneLoadListener loadListener) {
        removeLoadListener(loadListener);

        GrapheneLoadListener wrappedListener = new GrapheneLoadListener() {
            @Override
            public void onLoadingStateChange(org.cef.browser.CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                if (browser == GrapheneWebViewWidget.this.browser) {
                    loadListener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
                }
            }

            @Override
            public void onLoadStart(org.cef.browser.CefBrowser browser, org.cef.browser.CefFrame frame, org.cef.network.CefRequest.TransitionType transitionType) {
                if (browser == GrapheneWebViewWidget.this.browser) {
                    loadListener.onLoadStart(browser, frame, transitionType);
                }
            }

            @Override
            public void onLoadEnd(org.cef.browser.CefBrowser browser, org.cef.browser.CefFrame frame, int httpStatusCode) {
                if (browser == GrapheneWebViewWidget.this.browser) {
                    loadListener.onLoadEnd(browser, frame, httpStatusCode);
                }
            }

            @Override
            public void onLoadError(
                    CefBrowser browser,
                    CefFrame frame,
                    CefLoadHandler.ErrorCode errorCode,
                    String errorText,
                    String failedUrl
            ) {
                if (browser == GrapheneWebViewWidget.this.browser) {
                    loadListener.onLoadError(browser, frame, errorCode, errorText, failedUrl);
                }
            }
        };

        loadListenerWrappers.put(loadListener, wrappedListener);
        GrapheneCefRuntime.getLoadEventBus().register(wrappedListener);
    }

    public void removeLoadListener(GrapheneLoadListener loadListener) {
        GrapheneLoadListener wrappedListener = loadListenerWrappers.remove(loadListener);
        if (wrappedListener != null) {
            GrapheneCefRuntime.getLoadEventBus().unregister(wrappedListener);
        }
    }

    public void loadUrl(String url) {
        browser.loadURL(url);
    }

    public void goBack() {
        browser.goBack();
    }

    public void goForward() {
        browser.goForward();
    }

    public void reload() {
        browser.reload();
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!pointerButtonDown && isMouseOver(mouseX, mouseY)) {
            updateBrowserMousePosition(mouseX, mouseY);
        }

        if (browser.isLoading()) {
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x66333333);
        }

        surface.updateFrame();
        surface.render(guiGraphics, getX(), getY(), getWidth(), getHeight());

        if (isMouseOver(mouseX, mouseY)) {
            guiGraphics.requestCursor(browser.getRequestedCursor());
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        // No narration for web view widget
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        updateBrowserMousePosition(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (!isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            return super.mouseClicked(mouseButtonEvent, isDoubleClick);
        }

        setFocused(true);
        pointerButtonDown = mouseButtonEvent.button() == 0;
        Point browserPoint = toBrowserPoint(mouseButtonEvent.x(), mouseButtonEvent.y());
        browser.mouseInteracted(browserPoint.x, browserPoint.y, 0, mouseButtonEvent.button(), true, isDoubleClick ? 2 : 1);
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        browser.setFocus(focused);
        if (!focused) {
            pointerButtonDown = false;
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0) {
            pointerButtonDown = false;
        }

        if (!isFocused()) {
            return false;
        }

        Point browserPoint = toBrowserPoint(mouseButtonEvent.x(), mouseButtonEvent.y());
        browser.mouseInteracted(browserPoint.x, browserPoint.y, 0, mouseButtonEvent.button(), false, 1);
        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (!isFocused()) {
            return false;
        }

        Point browserPoint = toBrowserPoint(mouseButtonEvent.x(), mouseButtonEvent.y());
        lastBrowserMouseX = browserPoint.x;
        lastBrowserMouseY = browserPoint.y;
        browser.mouseDragged(browserPoint.x, browserPoint.y, mouseButtonEvent.button());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        Point browserPoint = toBrowserPoint(mouseX, mouseY);
        int delta = (int) (scrollY * 120);
        browser.mouseScrolled(browserPoint.x, browserPoint.y, 0, delta, 1);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (!isFocused()) {
            return false;
        }

        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), true);
        return true;
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent keyEvent) {
        if (!isFocused()) {
            return false;
        }

        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), false);
        return true;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent characterEvent) {
        if (!isFocused()) {
            return false;
        }

        browser.keyTyped((char) characterEvent.codepoint(), characterEvent.modifiers());
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        surface.setSurfaceSize(width, height);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        surface.setSurfaceSize(width, getHeight());
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        surface.setSurfaceSize(getWidth(), height);
    }

    public void handleScreenResize() {
        surface.setSurfaceSize(getWidth(), getHeight());
    }

    @Override
    public void close() {
        for (GrapheneLoadListener wrappedListener : loadListenerWrappers.values()) {
            GrapheneCefRuntime.getLoadEventBus().unregister(wrappedListener);
        }
        loadListenerWrappers.clear();

        if (screen instanceof GrapheneScreenBridge screenBridge) {
            screenBridge.grapheneui$removeWebViewWidget(this);
        }

        GrapheneCore.surfaces().closeOwner(this);
    }

    private void updateBrowserMousePosition(double mouseX, double mouseY) {
        Point browserPoint = toBrowserPoint(mouseX, mouseY);
        if (browserPoint.x == lastBrowserMouseX && browserPoint.y == lastBrowserMouseY) {
            return;
        }

        lastBrowserMouseX = browserPoint.x;
        lastBrowserMouseY = browserPoint.y;
        browser.mouseMoved(browserPoint.x, browserPoint.y, 0);
    }

    private Point toBrowserPoint(double mouseX, double mouseY) {
        double localX = mouseX - getX();
        double localY = mouseY - getY();
        return surface.toBrowserPoint(localX, localY, getWidth(), getHeight());
    }
}
