package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.browser.BrowserCursor;
import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.api.browser.BrowserLoadCompleted;
import io.github.trethore.graphene.api.browser.BrowserLoadFailed;
import io.github.trethore.graphene.api.browser.BrowserLoadListener;
import io.github.trethore.graphene.api.browser.BrowserLoadStarted;
import io.github.trethore.graphene.api.browser.BrowserLoadingState;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserScrollInput;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import io.github.trethore.graphene.internal.bridge.BridgeBrowser;
import io.github.trethore.graphene.internal.bridge.GrapheneBridgeRuntime;
import io.github.trethore.graphene.internal.browser.GrapheneFrameBuffer;
import io.github.trethore.graphene.internal.event.GrapheneLoadEventBus;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserWindowless;
import org.cef.browser.CefDevToolsClient;
import org.cef.browser.CefPaintEvent;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneCefBrowserSession extends CefBrowserWindowless
    implements BrowserSession, BridgeBrowser, CefRenderHandler {
  private static final String BLANK_URL = "about:blank";
  private static final String INPUT_NAME = "input";
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefBrowserSession.class);

  private final BrowserOptions options;
  private final Object navigationLock = new Object();
  private final long nativeWindowHandle;
  private final GrapheneBridgeRuntime bridgeRuntime;
  private final GrapheneCefFileDialogRouting fileDialogRouting = new GrapheneCefFileDialogRouting();
  private final GrapheneLoadEventBus loadEventBus;
  private final GrapheneFrameBuffer frameBuffer = new GrapheneFrameBuffer();
  private final Component uiComponent = new Canvas();
  private final Rectangle viewRect;
  private final Consumer<GrapheneCefBrowserSession> closeCallback;
  private final Map<BrowserLoadListener, BrowserLoadListener> listenerAdapters =
      new IdentityHashMap<>();
  private final GrapheneBridge bridge;
  private final Object dragLock = new Object();
  private CefDragData activeDragData;
  private int activeDragMask = CefDragData.DragOperations.DRAG_OPERATION_NONE;
  private boolean dragTargetEntered;
  private BrowserKeyInput lastPressedKeyInput;
  private volatile BrowserCursor pageCursor = BrowserCursor.ARROW;
  private boolean browserOptionsInitialized;
  private RuntimeException browserOptionsFailure;
  private String pendingUrl;
  private volatile boolean closed;
  private volatile boolean focused;
  private volatile BrowserCursor requestedCursor = BrowserCursor.ARROW;

  GrapheneCefBrowserSession(
      CefClient client,
      String url,
      BrowserOptions options,
      int width,
      int height,
      long nativeWindowHandle,
      GrapheneBridgeRuntime bridgeRuntime,
      GrapheneLoadEventBus loadEventBus,
      Consumer<GrapheneCefBrowserSession> closeCallback) {
    super(client, initialBrowserUrl(url, options), null, cefSettings(options));
    this.options = Objects.requireNonNull(options, "options");
    this.browserOptionsInitialized = !GrapheneCefBrowserOptions.requiresInitialization(options);
    this.pendingUrl = browserOptionsInitialized ? null : requireUrl(url);
    this.nativeWindowHandle = nativeWindowHandle;
    this.bridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
    this.loadEventBus = Objects.requireNonNull(loadEventBus, "loadEventBus");
    this.closeCallback = Objects.requireNonNull(closeCallback, "closeCallback");
    this.viewRect = new Rectangle(0, 0, requireDimension(width), requireDimension(height));
    this.bridge = bridgeRuntime.attach(this);
    try {
      fileDialogRouting.attach(bridge);
      createImmediately();
    } catch (RuntimeException exception) {
      fileDialogRouting.close();
      bridgeRuntime.detach(this);
      throw exception;
    }
  }

  @Override
  public void createImmediately() {
    setCloseAllowed();
    createBrowser(
        getClient(),
        nativeWindowHandle,
        getUrl(),
        true,
        options.transparent(),
        null,
        getRequestContext());
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
    synchronized (viewRect) {
      return new Rectangle(viewRect);
    }
  }

  @Override
  public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
    Rectangle bounds = getViewRect(browser);
    screenInfo.Set(1.0, 32, 8, false, bounds, bounds);
    return true;
  }

  @Override
  public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
    return viewPoint == null ? new Point() : new Point(viewPoint);
  }

  @Override
  public void onPopupShow(CefBrowser browser, boolean show) {
    if (!show) {
      frameBuffer.closePopup();
      invalidate();
    }
  }

  @Override
  public void onPopupSize(CefBrowser browser, Rectangle size) {
    if (size != null && size.x >= 0 && size.y >= 0 && size.width > 0 && size.height > 0) {
      frameBuffer.setPopupBounds(new BrowserDirtyRegion(size.x, size.y, size.width, size.height));
    }
    invalidate();
  }

  @Override
  public void onPaint(
      CefBrowser browser,
      boolean popup,
      Rectangle[] dirtyRects,
      ByteBuffer buffer,
      int width,
      int height) {
    if (closed) {
      return;
    }
    ByteBuffer pixels = buffer.duplicate();
    pixels.position(0);
    pixels.limit(Math.multiplyExact(Math.multiplyExact(width, height), 4));
    if (popup) {
      frameBuffer.capturePopup(width, height, pixels);
      return;
    }
    List<BrowserDirtyRegion> regions =
        dirtyRects == null
            ? List.of()
            : Arrays.stream(dirtyRects)
                .filter(Objects::nonNull)
                .map(
                    rectangle ->
                        new BrowserDirtyRegion(
                            rectangle.x, rectangle.y, rectangle.width, rectangle.height))
                .toList();
    frameBuffer.capture(width, height, regions, pixels);
  }

  @Override
  public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
    // Frames are exposed through BrowserSession.latestFrame().
  }

  @Override
  public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
    // Frames are exposed through BrowserSession.latestFrame().
  }

  @Override
  public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
    // No secondary paint listeners are retained.
  }

  @Override
  public boolean onCursorChange(CefBrowser browser, int cursorType) {
    BrowserCursor cursor = cursor(cursorType);
    pageCursor = cursor;
    synchronized (dragLock) {
      if (activeDragData == null) {
        requestedCursor = cursor;
      }
    }
    return true;
  }

  @Override
  public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
    if (dragData == null) {
      return false;
    }
    synchronized (dragLock) {
      closeActiveDragLocked();
      activeDragData = dragData.clone();
      activeDragMask = mask;
      dragTargetEntered = false;
      requestedCursor = BrowserCursor.HAND;
    }
    return true;
  }

  @Override
  public void updateDragCursor(CefBrowser browser, int operation) {
    synchronized (dragLock) {
      if (activeDragData != null) {
        requestedCursor =
            operation == CefDragData.DragOperations.DRAG_OPERATION_NONE
                ? BrowserCursor.NOT_ALLOWED
                : BrowserCursor.HAND;
      }
    }
  }

  @Override
  public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
    BrowserFrame frame = latestFrame();
    if (frame == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("No browser frame is available"));
    }
    BufferedImage image =
        new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_INT_ARGB);
    ByteBuffer pixels = frame.pixels();
    for (int y = 0; y < frame.height(); y++) {
      for (int x = 0; x < frame.width(); x++) {
        int blue = Byte.toUnsignedInt(pixels.get());
        int green = Byte.toUnsignedInt(pixels.get());
        int red = Byte.toUnsignedInt(pixels.get());
        int alpha = Byte.toUnsignedInt(pixels.get());
        image.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
      }
    }
    return CompletableFuture.completedFuture(image);
  }

  @Override
  protected CefBrowserWindowless createDevToolsBrowserWindowless(
      CefClient client,
      String url,
      CefRequestContext context,
      CefBrowserWindowless parent,
      Point inspectAt) {
    throw new UnsupportedOperationException("DevTools browser sessions are not implemented yet");
  }

  @Override
  public BrowserOptions options() {
    return options;
  }

  @Override
  public String currentUrl() {
    String url = getURL();
    return url == null || url.isBlank() ? BLANK_URL : url;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public BrowserCursor requestedCursor() {
    return requestedCursor;
  }

  @Override
  public void navigate(String url) {
    String validatedUrl = requireUrl(url);
    synchronized (navigationLock) {
      if (browserOptionsFailure != null) {
        throw browserOptionsFailure;
      }
      if (!browserOptionsInitialized) {
        pendingUrl = validatedUrl;
        return;
      }
    }
    loadURL(validatedUrl);
  }

  @Override
  public void stopLoading() {
    stopLoad();
  }

  @Override
  public void executeScript(String script) {
    executeScript(script, currentUrl());
  }

  @Override
  public void executeScript(String script, String sourceUrl) {
    String validatedScript = Objects.requireNonNull(script, "script");
    String validatedSourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl");
    if (!options.javascriptEnabled()) {
      return;
    }
    executeJavaScript(validatedScript, validatedSourceUrl, 1);
  }

  @Override
  public boolean hasDocument() {
    return getMainFrame() != null && getMainFrame().isValid();
  }

  @Override
  public int identifier() {
    return getIdentifier();
  }

  @Override
  public void resize(int width, int height) {
    int validatedWidth = requireDimension(width);
    int validatedHeight = requireDimension(height);
    synchronized (viewRect) {
      viewRect.setSize(validatedWidth, validatedHeight);
    }
    wasResized(validatedWidth, validatedHeight);
  }

  @Override
  public void setFocused(boolean focused) {
    this.focused = focused;
    setFocus(focused);
  }

  void restoreFocusAfterNavigation() {
    if (!focused || closed) {
      return;
    }
    setFocus(false);
    setFocus(true);
  }

  boolean consumeDirectoryPickerIntent() {
    return fileDialogRouting.consumeDirectoryIntent();
  }

  void initializeBrowserOptions() {
    if (browserOptionsInitialized) {
      return;
    }
    CefDevToolsClient devToolsClient = getDevToolsClient();
    if (devToolsClient == null) {
      failBrowserOptionInitialization(
          new IllegalStateException("JCEF DevTools client is unavailable"));
      return;
    }
    GrapheneCefBrowserOptions.apply(devToolsClient, options)
        .whenComplete(
            (ignored, failure) -> {
              if (failure != null) {
                failBrowserOptionInitialization(failure);
                return;
              }
              completeBrowserOptionInitialization();
            });
  }

  @Override
  public void sendPointerInput(BrowserPointerInput input) {
    BrowserPointerInput validatedInput = Objects.requireNonNull(input, INPUT_NAME);
    if (handleDragInput(validatedInput)) {
      return;
    }
    sendCefMouseEvent(GrapheneCefInputTranslator.pointer(validatedInput));
  }

  @Override
  public void sendScrollInput(BrowserScrollInput input) {
    sendCefMouseWheelEvent(
        GrapheneCefInputTranslator.scroll(Objects.requireNonNull(input, INPUT_NAME)));
  }

  @Override
  public void sendKeyInput(BrowserKeyInput input) {
    BrowserKeyInput validatedInput = Objects.requireNonNull(input, INPUT_NAME);
    if (validatedInput.action() == BrowserKeyAction.PRESS) {
      lastPressedKeyInput = validatedInput;
    }
    sendCefKeyEvent(GrapheneCefInputTranslator.key(validatedInput));
    if (validatedInput.action() == BrowserKeyAction.RELEASE) {
      lastPressedKeyInput = null;
    }
  }

  @Override
  public void sendTextInput(BrowserTextInput input) {
    sendCefKeyEvent(
        GrapheneCefInputTranslator.text(
            Objects.requireNonNull(input, INPUT_NAME), lastPressedKeyInput));
  }

  @Override
  public BrowserFrame latestFrame() {
    return frameBuffer.latestFrame();
  }

  @Override
  public GrapheneBridge bridge() {
    return bridge;
  }

  @Override
  public synchronized void addLoadListener(BrowserLoadListener listener) {
    BrowserLoadListener validatedListener = Objects.requireNonNull(listener, "listener");
    if (listenerAdapters.containsKey(validatedListener)) {
      return;
    }
    BrowserLoadListener adapter = new SessionLoadListener(validatedListener);
    listenerAdapters.put(validatedListener, adapter);
    loadEventBus.register(adapter);
  }

  @Override
  public synchronized void removeLoadListener(BrowserLoadListener listener) {
    BrowserLoadListener adapter =
        listenerAdapters.remove(Objects.requireNonNull(listener, "listener"));
    if (adapter != null) {
      loadEventBus.unregister(adapter);
    }
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    synchronized (navigationLock) {
      pendingUrl = null;
    }
    listenerAdapters.values().forEach(loadEventBus::unregister);
    listenerAdapters.clear();
    synchronized (dragLock) {
      closeActiveDragLocked();
    }
    fileDialogRouting.close();
    bridgeRuntime.detach(this);
    frameBuffer.clear();
    try {
      super.close(true);
    } finally {
      closeCallback.accept(this);
    }
  }

  private static CefBrowserSettings cefSettings(BrowserOptions options) {
    BrowserOptions validatedOptions = Objects.requireNonNull(options, "options");
    CefBrowserSettings settings = new CefBrowserSettings();
    settings.windowless_frame_rate = validatedOptions.maximumFrameRate();
    return settings;
  }

  private static String initialBrowserUrl(String url, BrowserOptions options) {
    String validatedUrl = requireUrl(url);
    return GrapheneCefBrowserOptions.requiresInitialization(options) ? BLANK_URL : validatedUrl;
  }

  private void completeBrowserOptionInitialization() {
    String targetUrl;
    synchronized (navigationLock) {
      if (closed) {
        return;
      }
      browserOptionsInitialized = true;
      targetUrl = pendingUrl;
      pendingUrl = null;
    }
    if (targetUrl != null) {
      loadURL(targetUrl);
    }
  }

  private void failBrowserOptionInitialization(Throwable failure) {
    RuntimeException exception =
        new IllegalStateException("Failed to apply browser options", failure);
    String targetUrl;
    synchronized (navigationLock) {
      if (closed) {
        return;
      }
      browserOptionsFailure = exception;
      targetUrl = pendingUrl;
      pendingUrl = null;
    }
    LOGGER.error("Failed to apply browser options for {}", targetUrl, failure);
  }

  private static String requireUrl(String url) {
    String validatedUrl = Objects.requireNonNull(url, "url").trim();
    if (validatedUrl.isBlank()) {
      throw new IllegalArgumentException("url must not be blank");
    }
    return validatedUrl;
  }

  private static int requireDimension(int dimension) {
    if (dimension <= 0) {
      throw new IllegalArgumentException("Browser dimensions must be positive");
    }
    return dimension;
  }

  private static BrowserCursor cursor(int cursorType) {
    return switch (cursorType) {
      case Cursor.CROSSHAIR_CURSOR -> BrowserCursor.CROSSHAIR;
      case Cursor.TEXT_CURSOR -> BrowserCursor.TEXT;
      case Cursor.HAND_CURSOR -> BrowserCursor.HAND;
      case Cursor.N_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR -> BrowserCursor.RESIZE_VERTICAL;
      case Cursor.E_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR -> BrowserCursor.RESIZE_HORIZONTAL;
      case Cursor.NE_RESIZE_CURSOR,
          Cursor.NW_RESIZE_CURSOR,
          Cursor.SE_RESIZE_CURSOR,
          Cursor.SW_RESIZE_CURSOR,
          Cursor.MOVE_CURSOR ->
          BrowserCursor.RESIZE_ALL;
      default -> BrowserCursor.ARROW;
    };
  }

  private boolean handleDragInput(BrowserPointerInput input) {
    synchronized (dragLock) {
      if (activeDragData == null) {
        return false;
      }
      Point point = new Point(input.x(), input.y());
      int modifiers = GrapheneCefInputTranslator.modifiers(input.modifiers());
      switch (input.action()) {
        case DRAG, MOVE -> {
          if (!dragTargetEntered) {
            dragTargetDragEnter(activeDragData, point, modifiers, activeDragMask);
            dragTargetEntered = true;
          } else {
            dragTargetDragOver(point, modifiers, activeDragMask);
          }
          return true;
        }
        case RELEASE -> {
          if (!dragTargetEntered) {
            dragTargetDragEnter(activeDragData, point, modifiers, activeDragMask);
          }
          dragTargetDrop(point, modifiers);
          dragSourceEndedAt(point, preferredDragOperation(activeDragMask));
          dragSourceSystemDragEnded();
          clearActiveDragLocked();
          return true;
        }
        case EXIT -> {
          closeActiveDragLocked();
          return true;
        }
        default -> {
          return true;
        }
      }
    }
  }

  private void closeActiveDragLocked() {
    if (activeDragData == null) {
      return;
    }
    if (dragTargetEntered) {
      dragTargetDragLeave();
    }
    dragSourceSystemDragEnded();
    clearActiveDragLocked();
  }

  private void clearActiveDragLocked() {
    if (activeDragData != null) {
      activeDragData.dispose();
      activeDragData = null;
    }
    activeDragMask = CefDragData.DragOperations.DRAG_OPERATION_NONE;
    dragTargetEntered = false;
    requestedCursor = pageCursor;
  }

  private static int preferredDragOperation(int mask) {
    if ((mask & CefDragData.DragOperations.DRAG_OPERATION_MOVE) != 0) {
      return CefDragData.DragOperations.DRAG_OPERATION_MOVE;
    }
    if ((mask & CefDragData.DragOperations.DRAG_OPERATION_COPY) != 0) {
      return CefDragData.DragOperations.DRAG_OPERATION_COPY;
    }
    if ((mask & CefDragData.DragOperations.DRAG_OPERATION_LINK) != 0) {
      return CefDragData.DragOperations.DRAG_OPERATION_LINK;
    }
    return CefDragData.DragOperations.DRAG_OPERATION_NONE;
  }

  private final class SessionLoadListener implements BrowserLoadListener {
    private final BrowserLoadListener delegate;

    private SessionLoadListener(BrowserLoadListener delegate) {
      this.delegate = delegate;
    }

    @Override
    public void onLoadingStateChanged(BrowserLoadingState state) {
      if (state.browserId() == identifier()) {
        delegate.onLoadingStateChanged(state);
      }
    }

    @Override
    public void onLoadStarted(BrowserLoadStarted event) {
      if (event.browserId() == identifier()) {
        delegate.onLoadStarted(event);
      }
    }

    @Override
    public void onLoadCompleted(BrowserLoadCompleted event) {
      if (event.browserId() == identifier()) {
        delegate.onLoadCompleted(event);
      }
    }

    @Override
    public void onLoadFailed(BrowserLoadFailed event) {
      if (event.browserId() == identifier()) {
        delegate.onLoadFailed(event);
      }
    }
  }
}
