package tytoo.grapheneui.browser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.bridge.GrapheneBridge;
import tytoo.grapheneui.cef.GrapheneClasspathUrls;
import tytoo.grapheneui.event.GrapheneLoadListener;
import tytoo.grapheneui.screen.GrapheneScreenBridge;

import java.awt.*;
import java.io.Closeable;
import java.util.Objects;

public class GrapheneWebViewWidget extends AbstractWidget implements Closeable {
    private static final String DEFAULT_URL = GrapheneClasspathUrls.asset("graphene_test/welcome.html");

    private final Screen screen;
    private final BrowserSurface surface;
    private final GrapheneBrowser browser;
    private final GrapheneWebViewInputController inputController;

    @SuppressWarnings("unused")
    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message) {
        this(screen, x, y, width, height, message, DEFAULT_URL);
    }

    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message, String url) {
        this(screen, x, y, width, height, message, url, BrowserSurfaceConfig.defaults());
    }

    public GrapheneWebViewWidget(
            Screen screen,
            int x,
            int y,
            int width,
            int height,
            Component message,
            String url,
            BrowserSurfaceConfig config
    ) {
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
                        .config(Objects.requireNonNull(config, "config"))
                        .build()
        );
    }

    public GrapheneWebViewWidget(Screen screen, int x, int y, int width, int height, Component message, BrowserSurface surface) {
        super(x, y, width, height, message);
        this.screen = screen;
        this.surface = Objects.requireNonNull(surface, "surface");
        this.browser = this.surface.browser();
        this.inputController = new GrapheneWebViewInputController(this.browser);

        if (!(screen instanceof GrapheneScreenBridge screenBridge)) {
            throw new IllegalStateException("Screen does not implement GrapheneScreenBridge: " + screen.getClass().getName());
        }

        screenBridge.grapheneui$addWebViewWidget(this);
        this.surface.registerTo(this);
        browser.setFocus(true);
        this.surface.setSurfaceSize(width, height);
    }

    public GrapheneBrowser getBrowser() {
        return browser;
    }

    public BrowserSurface getSurface() {
        return surface;
    }

    public GrapheneBridge bridge() {
        return surface.bridge();
    }

    @SuppressWarnings("unused")
    public void addLoadListener(GrapheneLoadListener loadListener) {
        surface.addLoadListener(loadListener);
    }

    public BrowserSurface.Subscription subscribeLoadListener(GrapheneLoadListener loadListener) {
        return surface.subscribeLoadListener(loadListener);
    }

    public void removeLoadListener(GrapheneLoadListener loadListener) {
        surface.removeLoadListener(loadListener);
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
        if (!inputController.isPrimaryPointerButtonDown() && isMouseOver(mouseX, mouseY)) {
            inputController.updateMousePosition(toBrowserPoint(mouseX, mouseY));
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
        inputController.updateMousePosition(toBrowserPoint(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (!isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            return super.mouseClicked(mouseButtonEvent, isDoubleClick);
        }

        setFocused(true);
        inputController.onMouseClicked(mouseButtonEvent.button(), isDoubleClick, toBrowserPoint(mouseButtonEvent.x(), mouseButtonEvent.y()));
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        browser.setFocus(focused);
        inputController.onFocusChanged(focused);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        return inputController.onMouseReleased(mouseButtonEvent.button(), toBrowserPoint(mouseButtonEvent.x(), mouseButtonEvent.y()), isFocused());
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return inputController.onMouseDragged(mouseButtonEvent.button(), toBrowserPoint(mouseButtonEvent.x(), mouseButtonEvent.y()), isFocused());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        Point browserPoint = toBrowserPoint(mouseX, mouseY);
        int delta = (int) (scrollY * 120);
        inputController.onMouseScrolled(browserPoint, delta, 1);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (!isFocused()) {
            return false;
        }

        inputController.onKeyPressed(keyEvent);
        return true;
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent keyEvent) {
        if (!isFocused()) {
            return false;
        }

        inputController.onKeyReleased(keyEvent);
        return true;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent characterEvent) {
        if (!isFocused()) {
            return false;
        }

        inputController.onCharacterTyped(characterEvent);
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
        if (screen instanceof GrapheneScreenBridge screenBridge) {
            screenBridge.grapheneui$removeWebViewWidget(this);
        }

        GrapheneCore.surfaces().closeOwner(this);
    }

    private Point toBrowserPoint(double mouseX, double mouseY) {
        double localX = mouseX - getX();
        double localY = mouseY - getY();
        return surface.toBrowserPoint(localX, localY, getWidth(), getHeight());
    }
}
