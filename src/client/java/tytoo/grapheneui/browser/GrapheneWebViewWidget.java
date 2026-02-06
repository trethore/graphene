package tytoo.grapheneui.browser;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.cef.browser.CefRequestContext;
import tytoo.grapheneui.cef.GrapheneCefRuntime;
import tytoo.grapheneui.event.GrapheneLoadListener;
import tytoo.grapheneui.render.GrapheneLwjglRenderer;
import tytoo.grapheneui.screen.GrapheneScreenBridge;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class GrapheneWebViewWidget extends AbstractWidget implements Closeable {
    private final Screen screen;
    private final GrapheneLwjglRenderer renderer;
    private final GrapheneBrowser browser;
    private final Map<GrapheneLoadListener, GrapheneLoadListener> loadListenerWrappers = new HashMap<>();

    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message) {
        this(screen, x, y, width, height, message, "classpath://assets/graphene-ui/graphene_test/welcome.html");
    }

    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message, String url) {
        super(x, y, width, height, message);
        this.screen = screen;
        this.renderer = new GrapheneLwjglRenderer(true);
        this.browser = new GrapheneBrowser(
                GrapheneCefRuntime.requireClient(),
                url,
                true,
                CefRequestContext.getGlobalContext(),
                renderer
        );

        if (!(screen instanceof GrapheneScreenBridge screenBridge)) {
            throw new IllegalStateException("Screen does not implement GrapheneScreenBridge: " + screen.getClass().getName());
        }

        screenBridge.grapheneui$addWebViewWidget(this);
        browser.createImmediately();
        browser.setFocus(true);
        browser.setCloseAllowed();
        resizeBrowser(width, height);
    }

    public GrapheneBrowser getBrowser() {
        return browser;
    }

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
                    org.cef.browser.CefBrowser browser,
                    org.cef.browser.CefFrame frame,
                    org.cef.handler.CefLoadHandler.ErrorCode errorCode,
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
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (browser.isLoading()) {
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x66333333);
        }

        browser.updateRendererFrame();
        browser.renderTo(getX(), getY(), getWidth(), getHeight(), guiGraphics);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        int localX = (int) ((mouseX - getX()) * getScaleX());
        int localY = (int) ((mouseY - getY()) * getScaleY());
        browser.mouseMoved(localX, localY, 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (!isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            return super.mouseClicked(mouseButtonEvent, isDoubleClick);
        }

        setFocused(true);
        int localX = (int) ((mouseButtonEvent.x() - getX()) * getScaleX());
        int localY = (int) ((mouseButtonEvent.y() - getY()) * getScaleY());
        browser.mouseInteracted(localX, localY, 0, mouseButtonEvent.button(), true, isDoubleClick ? 2 : 1);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (!isFocused()) {
            return false;
        }

        int localX = (int) ((mouseButtonEvent.x() - getX()) * getScaleX());
        int localY = (int) ((mouseButtonEvent.y() - getY()) * getScaleY());
        browser.mouseInteracted(localX, localY, 0, mouseButtonEvent.button(), false, 1);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (!isFocused()) {
            return false;
        }

        int localX = (int) ((mouseButtonEvent.x() - getX()) * getScaleX());
        int localY = (int) ((mouseButtonEvent.y() - getY()) * getScaleY());
        browser.mouseDragged(localX, localY, mouseButtonEvent.button(), dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int localX = (int) ((mouseX - getX()) * getScaleX());
        int localY = (int) ((mouseY - getY()) * getScaleY());
        int delta = (int) (scrollY * 120);
        browser.mouseScrolled(localX, localY, 0, delta, 1);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!isFocused()) {
            return false;
        }

        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), true);
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        if (!isFocused()) {
            return false;
        }

        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), false);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!isFocused()) {
            return false;
        }

        browser.keyTyped((char) characterEvent.codepoint(), characterEvent.modifiers());
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        resizeBrowser(width, height);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        resizeBrowser(width, getHeight());
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        resizeBrowser(getWidth(), height);
    }

    public void handleScreenResize() {
        resizeBrowser(getWidth(), getHeight());
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

        browser.close();
    }

    private void resizeBrowser(int width, int height) {
        int realPixelWidth = (int) Math.max(1.0, width * getScaleX());
        int realPixelHeight = (int) Math.max(1.0, height * getScaleY());
        browser.wasResizedTo(realPixelWidth, realPixelHeight);
    }

    private double getScaleX() {
        Window window = Minecraft.getInstance().getWindow();
        return window.getWidth() / (double) window.getGuiScaledWidth();
    }

    private double getScaleY() {
        Window window = Minecraft.getInstance().getWindow();
        return window.getHeight() / (double) window.getGuiScaledHeight();
    }
}
