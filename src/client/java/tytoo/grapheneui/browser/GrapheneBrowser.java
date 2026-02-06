package tytoo.grapheneui.browser;

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
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.input.GrapheneKeyCodeUtil;
import tytoo.grapheneui.render.GrapheneRenderer;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class GrapheneBrowser extends CefBrowserNAccessor implements CefRenderHandler, AutoCloseable {
    private static final int MOUSE_LEFT_BUTTON = 0;
    private static final int MOUSE_RIGHT_BUTTON = 1;
    private static final int MOUSE_MIDDLE_BUTTON = 2;
    private static final ScancodeInjector SCANCODE_INJECTOR = ScancodeInjector.create();

    private final long windowHandle = this.hashCode();
    private final GrapheneRenderer renderer;
    private final boolean transparent;
    private final Rectangle browserRect = new Rectangle(0, 0, 1, 1);
    private final Point screenPoint = new Point(0, 0);
    private final Component uiComponent = new Component() {
    };
    private final PaintData paintData = new PaintData();
    private final PopupData popupData = new PopupData();
    private boolean closed = false;

    public GrapheneBrowser(CefClient client, String url, boolean transparent, CefRequestContext context, GrapheneRenderer renderer) {
        this(client, url, transparent, context, renderer, null, null);
    }

    private GrapheneBrowser(
            CefClient client,
            String url,
            boolean transparent,
            CefRequestContext context,
            GrapheneRenderer renderer,
            CefBrowserNAccessor parent,
            Point inspectAt
    ) {
        super(client, url, context, parent, inspectAt, new CefBrowserSettings());
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
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            awtModifiers |= InputEvent.SHIFT_DOWN_MASK;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            awtModifiers |= InputEvent.CTRL_DOWN_MASK;
        }

        if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            awtModifiers |= InputEvent.ALT_DOWN_MASK;
        }

        if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0) {
            awtModifiers |= InputEvent.META_DOWN_MASK;
        }

        return awtModifiers;
    }

    private static int toAwtButtonDownModifier(int button) {
        return switch (remapMouseCode(button)) {
            case MouseEvent.BUTTON1 -> InputEvent.BUTTON1_DOWN_MASK;
            case MouseEvent.BUTTON2 -> InputEvent.BUTTON2_DOWN_MASK;
            case MouseEvent.BUTTON3 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> 0;
        };
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
        return new GrapheneBrowser(client, url == null ? "about:blank" : url, false, context, new NoopRenderer(), parent, inspectAt);
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

    public void renderTo(int x, int y, int width, int height, GuiGraphics guiGraphics) {
        renderer.render(guiGraphics, x, y, width, height);
    }

    public void wasResizedTo(int width, int height) {
        browserRect.setBounds(0, 0, width, height);
        super.wasResized(width, height);
    }

    @SuppressWarnings("MagicConstant")
    public void mouseMoved(int x, int y, int modifiers) {
        int awtModifiers = toAwtModifiers(modifiers);
        MouseEvent event = new MouseEvent(uiComponent, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), awtModifiers, x, y, 0, false);
        sendMouseEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    public void mouseDragged(double x, double y, int button) {
        int awtModifiers = toAwtButtonDownModifier(button);
        MouseEvent event = new MouseEvent(uiComponent, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), awtModifiers, (int) x, (int) y, 1, true);
        sendMouseEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    public void mouseInteracted(int x, int y, int modifiers, int button, boolean pressed, int clickCount) {
        int awtButton = remapMouseCode(button);
        int awtModifiers = toAwtModifiers(modifiers);
        MouseEvent event = new MouseEvent(
                uiComponent,
                pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                awtModifiers,
                x,
                y,
                clickCount,
                false,
                awtButton
        );
        sendMouseEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    public void mouseScrolled(int x, int y, int modifiers, int amount, int rotation) {
        int awtModifiers = toAwtModifiers(modifiers);
        MouseWheelEvent event = new MouseWheelEvent(
                uiComponent,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                awtModifiers,
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

    @SuppressWarnings("MagicConstant")
    public void keyTyped(char character, int modifiers) {
        int awtModifiers = toAwtModifiers(modifiers);
        KeyEvent event = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, character);
        sendKeyEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    public void keyEventByKeyCode(int keyCode, int scanCode, int modifiers, boolean pressed) {
        int awtModifiers = toAwtModifiers(modifiers);
        int awtKeyCode = GrapheneKeyCodeUtil.toAwtKeyCode(keyCode);
        char character = GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);

        KeyEvent event = new KeyEvent(
                uiComponent,
                pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                awtModifiers,
                awtKeyCode,
                character == KeyEvent.CHAR_UNDEFINED ? 0 : character,
                KeyEvent.KEY_LOCATION_STANDARD
        );

        SCANCODE_INJECTOR.inject(event, scanCode);
        sendKeyEvent(event);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            KeyEvent typedBackspace = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, '\b');
            SCANCODE_INJECTOR.inject(typedBackspace, scanCode);
            sendKeyEvent(typedBackspace);
        }
    }

    public void onTitleChange(String title) {
        renderer.onTitleChange(title);
    }

    private void createBrowserIfRequired() {
        if (getNativeRef("CefBrowser") != 0L) {
            return;
        }

        if (getParentBrowser() == null) {
            createBrowser(getClient(), windowHandle, getUrl(), true, transparent, null, getRequestContext());
        }
    }

    private record ScancodeInjector(Object unsafe, MethodHandle putLongHandle, long scancodeOffset) {
        private static final long SCANCODE_UNSUPPORTED = -1L;

        private static ScancodeInjector create() {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                MethodHandles.Lookup unsafeLookup = MethodHandles.privateLookupIn(unsafeClass, MethodHandles.lookup());
                Object unsafe = unsafeLookup.findStaticVarHandle(unsafeClass, "theUnsafe", unsafeClass).get();

                MethodHandle objectFieldOffsetHandle = unsafeLookup.findVirtual(
                        unsafeClass,
                        "objectFieldOffset",
                        MethodType.methodType(long.class, Field.class)
                );
                MethodHandle putLongHandle = unsafeLookup.findVirtual(
                        unsafeClass,
                        "putLong",
                        MethodType.methodType(void.class, Object.class, long.class, long.class)
                );

                Field scancodeField = KeyEvent.class.getDeclaredField("scancode");
                long scancodeOffset = (long) objectFieldOffsetHandle.invoke(unsafe, scancodeField);
                return new ScancodeInjector(unsafe, putLongHandle, scancodeOffset);
            } catch (Throwable _) {
                return new ScancodeInjector(null, null, SCANCODE_UNSUPPORTED);
            }
        }

        private void inject(KeyEvent event, int scanCode) {
            if (!isSupported()) {
                return;
            }

            try {
                putLongHandle.invoke(unsafe, event, scancodeOffset, scanCode & 0xFFL);
            } catch (Throwable _) {
                // Ignore and keep dispatching key events without native scancode.
            }
        }

        private boolean isSupported() {
            return unsafe != null && putLongHandle != null && scancodeOffset >= 0L;
        }
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
        public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
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
        public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Noop renderer"));
        }
    }
}
