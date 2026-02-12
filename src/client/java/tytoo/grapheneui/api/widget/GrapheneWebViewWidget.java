package tytoo.grapheneui.api.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.surface.BrowserSurface;
import tytoo.grapheneui.api.surface.BrowserSurfaceConfig;
import tytoo.grapheneui.api.surface.BrowserSurfaceInputAdapter;
import tytoo.grapheneui.api.surface.GrapheneLoadListener;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;
import tytoo.grapheneui.internal.screen.GrapheneScreenBridge;

import java.io.Closeable;
import java.util.Objects;

public class GrapheneWebViewWidget extends AbstractWidget implements Closeable {
    private static final String DEFAULT_URL = GrapheneClasspathUrls.asset("graphene_test/welcome.html");

    private final Screen screen;
    private final BrowserSurface surface;
    private final BrowserSurfaceInputAdapter inputAdapter;

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
        this.inputAdapter = new BrowserSurfaceInputAdapter(this.surface);

        if (!(screen instanceof GrapheneScreenBridge screenBridge)) {
            throw new IllegalStateException("Screen does not implement GrapheneScreenBridge: " + screen.getClass().getName());
        }

        screenBridge.addGrapheneWebViewWidget(this);
        this.surface.setOwner(this);
        this.surface.setSurfaceSize(width, height);
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
        surface.loadUrl(url);
    }

    public void goBack() {
        surface.goBack();
    }

    public void goForward() {
        surface.goForward();
    }

    public void reload() {
        surface.reload();
    }

    public String currentUrl() {
        return surface.currentUrl();
    }

    public boolean canGoBack() {
        return surface.canGoBack();
    }

    public boolean canGoForward() {
        return surface.canGoForward();
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!inputAdapter.isPrimaryPointerButtonDown() && isMouseOver(mouseX, mouseY)) {
            inputAdapter.mouseMoved(localX(mouseX), localY(mouseY), getWidth(), getHeight());
        }

        if (surface.isLoading()) {
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x66333333);
        }

        surface.render(guiGraphics, getX(), getY(), getWidth(), getHeight());

        if (isMouseOver(mouseX, mouseY)) {
            guiGraphics.requestCursor(surface.getRequestedCursor());
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        // No narration for web view widget
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        inputAdapter.mouseMoved(localX(mouseX), localY(mouseY), getWidth(), getHeight());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (!isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            return super.mouseClicked(mouseButtonEvent, isDoubleClick);
        }

        setFocused(true);
        inputAdapter.mouseClicked(
                mouseButtonEvent.button(),
                isDoubleClick,
                localX(mouseButtonEvent.x()),
                localY(mouseButtonEvent.y()),
                getWidth(),
                getHeight()
        );
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        inputAdapter.setFocused(focused);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        return inputAdapter.mouseReleased(
                mouseButtonEvent.button(),
                localX(mouseButtonEvent.x()),
                localY(mouseButtonEvent.y()),
                getWidth(),
                getHeight()
        );
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return inputAdapter.mouseDragged(
                mouseButtonEvent.button(),
                localX(mouseButtonEvent.x()),
                localY(mouseButtonEvent.y()),
                getWidth(),
                getHeight()
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        inputAdapter.mouseScrolled(localX(mouseX), localY(mouseY), scrollY, getWidth(), getHeight());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        return inputAdapter.keyPressed(keyEvent);
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent keyEvent) {
        return inputAdapter.keyReleased(keyEvent);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent characterEvent) {
        return inputAdapter.charTyped(characterEvent);
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
            screenBridge.removeGrapheneWebViewWidget(this);
        }

        GrapheneCore.closeOwnedSurfaces(this);
    }

    private double localX(double mouseX) {
        return mouseX - getX();
    }

    private double localY(double mouseY) {
        return mouseY - getY();
    }
}
