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
import tytoo.grapheneui.api.surface.*;
import tytoo.grapheneui.internal.screen.GrapheneScreenBridge;

import java.io.Closeable;
import java.util.Objects;

/**
 * A widget that displays a web view using a {@link BrowserSurface}.
 * The widget handles rendering the surface and forwarding input events to it.
 */
public class GrapheneWebViewWidget extends AbstractWidget implements Closeable {
    private static final String DEFAULT_URL = "about:blank";

    private final Screen screen;
    private final BrowserSurface surface;
    private final BrowserSurfaceInputAdapter inputAdapter;
    private boolean closed;

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

        screenBridge.graphene$addWebViewWidget(this);
        this.surface.setOwner(this);
        this.surface.setSurfaceSize(width, height);
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (isClosed()) {
            return;
        }

        if (!inputAdapter.isPrimaryPointerButtonDown() && isMouseOver(mouseX, mouseY)) {
            inputAdapter.mouseMoved(localX(mouseX), localY(mouseY), getWidth(), getHeight());
        }

        if (surface.isLoading()) {
            drawLoadingOverlay(guiGraphics);
        }

        surface.render(guiGraphics, getX(), getY(), getWidth(), getHeight());

        if (isMouseOver(mouseX, mouseY) && !inputAdapter.isCursorCaptured()) {
            guiGraphics.requestCursor(surface.getRequestedCursor());
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        // No narration for web view widget
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isClosed()) {
            return;
        }

        super.mouseMoved(mouseX, mouseY);
        inputAdapter.mouseMoved(localX(mouseX), localY(mouseY), getWidth(), getHeight());
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (isClosed()) {
            return false;
        }

        if (!isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            return super.mouseClicked(mouseButtonEvent, isDoubleClick);
        }

        requestKeyboardFocus();
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
        if (isClosed()) {
            super.setFocused(false);
            return;
        }

        super.setFocused(focused);
        inputAdapter.setFocused(focused);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        if (isClosed()) {
            return false;
        }

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
        if (isClosed()) {
            return false;
        }

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
        if (isClosed()) {
            return false;
        }

        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        inputAdapter.mouseScrolled(localX(mouseX), localY(mouseY), scrollY, getWidth(), getHeight());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (isClosed()) {
            return false;
        }

        return inputAdapter.keyPressed(keyEvent);
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent keyEvent) {
        if (isClosed()) {
            return false;
        }

        return inputAdapter.keyReleased(keyEvent);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent characterEvent) {
        if (isClosed()) {
            return false;
        }

        return inputAdapter.charTyped(characterEvent);
    }

    @Override
    public void setSize(int width, int height) {
        ensureOpen();
        super.setSize(width, height);
        surface.setSurfaceSize(width, height);
    }

    @Override
    public void setWidth(int width) {
        ensureOpen();
        super.setWidth(width);
        surface.setSurfaceSize(width, getHeight());
    }

    @Override
    public void setHeight(int height) {
        ensureOpen();
        super.setHeight(height);
        surface.setSurfaceSize(getWidth(), height);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        if (screen.getFocused() == this) {
            screen.setFocused(null);
        } else {
            setFocused(false);
        }

        active = false;
        visible = false;
        closed = true;

        if (screen instanceof GrapheneScreenBridge screenBridge) {
            screenBridge.graphene$removeWebViewWidget(this);
        }

        GrapheneCore.closeOwnedSurfaces(this);
    }

    @SuppressWarnings("unused")
    public void addLoadListener(GrapheneLoadListener loadListener) {
        ensureOpen();
        surface.addLoadListener(loadListener);
    }

    public BrowserSurface.Subscription subscribeLoadListener(GrapheneLoadListener loadListener) {
        ensureOpen();
        return surface.subscribeLoadListener(loadListener);
    }

    public void removeLoadListener(GrapheneLoadListener loadListener) {
        ensureOpen();
        surface.removeLoadListener(loadListener);
    }

    public BrowserSurface.Subscription subscribeTitleListener(GrapheneTitleListener titleListener) {
        ensureOpen();
        return surface.subscribeTitleListener(titleListener);
    }

    public void addTitleListener(GrapheneTitleListener titleListener) {
        ensureOpen();
        surface.addTitleListener(titleListener);
    }

    public void removeTitleListener(GrapheneTitleListener titleListener) {
        ensureOpen();
        surface.removeTitleListener(titleListener);
    }

    public void loadUrl(String url) {
        ensureOpen();
        surface.loadUrl(url);
    }

    public void goBack() {
        ensureOpen();
        surface.goBack();
    }

    public void goForward() {
        ensureOpen();
        surface.goForward();
    }

    public void reload() {
        ensureOpen();
        surface.reload();
    }

    public void requestKeyboardFocus() {
        ensureOpen();
        // Screen#setFocused only updates the child when the focused widget changes.
        // If this web view is already the screen's focused widget, clicking an input inside it
        // still needs to reassert native CEF focus or some pages stop painting the caret.
        if (screen.getFocused() == this) {
            setFocused(true);
            return;
        }

        screen.setFocused(this);
    }

    public boolean isCursorCaptured() {
        return !isClosed() && inputAdapter.isCursorCaptured();
    }

    public void cursorCaptureMoved(double deltaX, double deltaY) {
        if (isClosed()) {
            return;
        }

        inputAdapter.cursorCaptureMoved(deltaX, deltaY, getWidth(), getHeight());
    }

    public boolean consumeScreenEscape(@NonNull KeyEvent keyEvent) {
        if (isClosed() || !inputAdapter.shouldConsumeScreenEscape()) {
            return false;
        }

        keyPressed(keyEvent);
        if (inputAdapter.shouldReleaseCaptureOnEscape()) {
            inputAdapter.releaseInputCapture("escape");
        }

        return true;
    }

    public void handleScreenResize() {
        if (isClosed()) {
            return;
        }

        surface.setSurfaceSize(getWidth(), getHeight());
    }

    /**
     * Draws a loading overlay while the browser surface is loading.
     *
     * <p>The default implementation is a no-op. Override this method to provide a custom loading
     * indicator.</p>
     */
    protected void drawLoadingOverlay(@SuppressWarnings("unused") @NonNull GuiGraphics guiGraphics) {
        // Intentionally empty: subclasses can override this hook to draw custom loading UI.
    }

    private void ensureOpen() {
        if (isClosed()) {
            throw new IllegalStateException("GrapheneWebViewWidget is closed");
        }
    }

    private double localX(double mouseX) {
        return mouseX - getX();
    }

    private double localY(double mouseY) {
        return mouseY - getY();
    }

    public BrowserSurface getSurface() {
        ensureOpen();
        return surface;
    }

    public boolean isClosed() {
        return closed || surface.isClosed();
    }

    public GrapheneBridge bridge() {
        ensureOpen();
        return surface.bridge();
    }

    public String currentUrl() {
        ensureOpen();
        return surface.currentUrl();
    }

    public String currentTitle() {
        ensureOpen();
        return surface.currentTitle();
    }

    public boolean canGoBack() {
        ensureOpen();
        return surface.canGoBack();
    }

    public boolean canGoForward() {
        ensureOpen();
        return surface.canGoForward();
    }
}
