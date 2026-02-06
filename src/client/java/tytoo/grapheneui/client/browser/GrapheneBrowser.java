package tytoo.grapheneui.client.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserNAccessor;
import org.cef.browser.CefPaintEvent;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.lwjgl.glfw.GLFW;
import sun.misc.Unsafe;
import tytoo.grapheneui.client.input.GrapheneKeyCodeUtil;
import tytoo.grapheneui.client.render.GrapheneRenderer;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class GrapheneBrowser extends CefBrowserNAccessor implements CefRenderHandler, AutoCloseable {
    private static final int MOUSE_LEFT_BUTTON = 0;
    private static final int MOUSE_RIGHT_BUTTON = 1;
    private static final int MOUSE_MIDDLE_BUTTON = 2;
    @SuppressWarnings("removal")
    private static final Unsafe UNSAFE = resolveUnsafe();
    private static final long KEY_EVENT_SCANCODE_OFFSET = resolveScancodeOffset();

    private final long windowHandle = this.hashCode();
    private final GrapheneRenderer renderer;
    private final boolean transparent;
    private final Rectangle browserRect = new Rectangle(0, 0, 1, 1);
    private final Point screenPoint = new Point(0, 0);
    private final Component uiComponent = new Component() {
    };
    private final PaintData paintData = new PaintData();
    private final PopupData popupData = new PopupData();
    private boolean justCreated = false;
    private boolean closed = false;
    private String title = "Loading";

    public GrapheneBrowser(CefClient client, String url, boolean transparent, CefRequestContext context, GrapheneRenderer renderer) {
        this(client, url, transparent, context, renderer, null, null, new CefBrowserSettings());
    }

    private GrapheneBrowser(
            CefClient client,
            String url,
            boolean transparent,
            CefRequestContext context,
            GrapheneRenderer renderer,
            CefBrowserNAccessor parent,
            Point inspectAt,
            CefBrowserSettings settings
    ) {
        super(client, url, context, parent, inspectAt, settings);
        this.transparent = transparent;
        this.renderer = renderer;
    }

    private static int remapMouseCode(int button) {
        return switch (button) {
            case MOUSE_LEFT_BUTTON -> MouseEvent.BUTTON1;
            case MOUSE_RIGHT_BUTTON -> MouseEvent.BUTTON3;
            case MOUSE_MIDDLE_BUTTON -> MouseEvent.BUTTON2;
            default -> MouseEvent.NOBUTTON;
        };
    }

    private static int toAwtModifiers(int modifiers) {
        int awtModifiers = 0;
        if ((modifiers & 1) != 0) {
            awtModifiers |= InputEvent.SHIFT_DOWN_MASK;
        }

        if ((modifiers & 2) != 0) {
            awtModifiers |= InputEvent.CTRL_DOWN_MASK;
        }

        if ((modifiers & 4) != 0) {
            awtModifiers |= InputEvent.ALT_DOWN_MASK;
        }

        return awtModifiers;
    }

    @SuppressWarnings("removal")
    private static Unsafe resolveUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static long resolveScancodeOffset() {
        if (UNSAFE == null) {
            return -1L;
        }

        try {
            Field scancodeField = KeyEvent.class.getDeclaredField("scancode");
            return UNSAFE.objectFieldOffset(scancodeField);
        } catch (NoSuchFieldException exception) {
            return -1L;
        }
    }

    private static void applyScancode(KeyEvent event, int scancode) {
        if (UNSAFE == null || KEY_EVENT_SCANCODE_OFFSET < 0L) {
            return;
        }

        UNSAFE.putInt(event, KEY_EVENT_SCANCODE_OFFSET, scancode & 0xFF);
    }

    @Override
    public void createImmediately() {
        justCreated = true;
        setCloseAllowed();
        createBrowserIfRequired(false);
    }

    @Override
    public synchronized void onBeforeClose() {
        super.onBeforeClose();
        renderer.destroy();
    }

    @Override
    public Component getUIComponent() {
        return uiComponent;
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
        if (popup) {
            synchronized (popupData) {
                int size = (width * height) << 2;
                if (popupData.buffer == null || popupData.buffer.capacity() != size) {
                    popupData.buffer = ByteBuffer.allocateDirect(size);
                }

                buffer.position(0);
                popupData.buffer.position(0);
                popupData.buffer.put(buffer);
                popupData.buffer.position(0);

                popupData.width = width;
                popupData.height = height;
                popupData.hasFrame = true;
            }

            return;
        }

        synchronized (paintData) {
            int size = (width * height) << 2;
            if (paintData.buffer == null || paintData.buffer.capacity() != size) {
                paintData.buffer = ByteBuffer.allocateDirect(size);
            }

            if (paintData.hasFrame) {
                paintData.fullReRender = true;
            }

            paintData.buffer.position(0);
            paintData.buffer.limit(buffer.limit());
            buffer.position(0);
            paintData.buffer.put(buffer);
            paintData.buffer.position(0);

            paintData.width = width;
            paintData.height = height;
            paintData.dirtyRects = dirtyRects;
            paintData.hasFrame = true;
        }
    }

    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
    }

    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
    }

    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        return true;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return true;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return renderer.createScreenshot(nativeResolution);
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
    protected CefBrowserNAccessor createDevToolsBrowserAccessor(
            CefClient client,
            String url,
            CefRequestContext context,
            CefBrowserNAccessor parent,
            Point inspectAt
    ) {
        return new GrapheneBrowser(client, url == null ? "about:blank" : url, false, context, new NoopRenderer(), parent, inspectAt, new CefBrowserSettings());
    }

    public void updateRendererFrame() {
        synchronized (paintData) {
            if (paintData.hasFrame) {
                renderer.onPaint(false, paintData.dirtyRects, paintData.buffer, paintData.width, paintData.height, paintData.fullReRender);
                paintData.hasFrame = false;
                paintData.fullReRender = false;
            }
        }

        synchronized (popupData) {
            if (popupData.hasFrame) {
                renderer.onPaint(true, null, popupData.buffer, popupData.width, popupData.height, false);
                popupData.hasFrame = false;
            }
        }
    }

    public void renderTo(int x, int y, int width, int height, net.minecraft.client.gui.GuiGraphics guiGraphics) {
        renderer.render(guiGraphics, x, y, width, height);
    }

    public void wasResizedTo(int width, int height) {
        browserRect.setBounds(0, 0, width, height);
        super.wasResized(width, height);
    }

    public void mouseMoved(int x, int y, int modifiers) {
        MouseEvent event = new MouseEvent(uiComponent, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), modifiers, x, y, 0, false);
        sendMouseEvent(event);
    }

    public void mouseDragged(double x, double y, int button, double dragX, double dragY) {
        MouseEvent event = new MouseEvent(uiComponent, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, (int) x, (int) y, 1, true);
        sendMouseEvent(event);
    }

    public void mouseInteracted(int x, int y, int modifiers, int button, boolean pressed, int clickCount) {
        int awtButton = remapMouseCode(button);
        MouseEvent event = new MouseEvent(
                uiComponent,
                pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                modifiers,
                x,
                y,
                clickCount,
                false,
                awtButton
        );
        sendMouseEvent(event);
    }

    public void mouseScrolled(int x, int y, int modifiers, int amount, int rotation) {
        MouseWheelEvent event = new MouseWheelEvent(
                uiComponent,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                modifiers,
                x,
                y,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                amount,
                rotation
        );
        sendMouseWheelEvent(event);
    }

    public void keyTyped(char character, int modifiers) {
        int awtModifiers = toAwtModifiers(modifiers);
        KeyEvent event = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, character);
        sendKeyEvent(event);
    }

    public void keyEventByKeyCode(int keyCode, int scanCode, int modifiers, boolean pressed) {
        int awtModifiers = toAwtModifiers(modifiers);
        int awtKeyCode = GrapheneKeyCodeUtil.toAwtKeyCode(keyCode);
        char character = GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & 1) != 0);

        KeyEvent event = new KeyEvent(
                uiComponent,
                pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                awtModifiers,
                awtKeyCode,
                character == KeyEvent.CHAR_UNDEFINED ? 0 : character,
                KeyEvent.KEY_LOCATION_STANDARD
        );

        applyScancode(event, scanCode);
        sendKeyEvent(event);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            KeyEvent typedBackspace = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, '\b');
            applyScancode(typedBackspace, scanCode);
            sendKeyEvent(typedBackspace);
        }
    }

    public String getTitle() {
        return title;
    }

    public void onTitleChange(String title) {
        this.title = title;
        renderer.onTitleChange(title);
    }

    private void createBrowserIfRequired(boolean hasParent) {
        long currentWindowHandle = windowHandle;
        if (getParentBrowser() != null) {
            currentWindowHandle = getWindowHandle();
        }

        if (getNativeRef("CefBrowser") == 0L) {
            if (getParentBrowser() == null) {
                createBrowser(getClient(), currentWindowHandle, getUrl(), true, transparent, null, getRequestContext());
            }
        } else if (hasParent && justCreated) {
            notifyAfterParentChanged();
            setFocus(true);
            justCreated = false;
        }
    }

    private void notifyAfterParentChanged() {
        getClient().onAfterParentChanged(this);
    }

    private long getWindowHandle() {
        return windowHandle;
    }

    private static final class PaintData {
        private ByteBuffer buffer;
        private int width;
        private int height;
        private Rectangle[] dirtyRects;
        private boolean hasFrame;
        private boolean fullReRender;
    }

    private static final class PopupData {
        private ByteBuffer buffer;
        private int width;
        private int height;
        private boolean hasFrame;
    }

    private static final class NoopRenderer implements GrapheneRenderer {
        @Override
        public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int x, int y, int width, int height) {
        }

        @Override
        public void destroy() {
        }

        @Override
        public void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender) {
        }

        @Override
        public void onPopupSize(Rectangle rect) {
        }

        @Override
        public void onPopupClosed() {
        }

        @Override
        public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Noop renderer"));
        }
    }
}
