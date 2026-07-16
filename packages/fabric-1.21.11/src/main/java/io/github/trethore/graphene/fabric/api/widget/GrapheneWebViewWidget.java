package io.github.trethore.graphene.fabric.api.widget;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.fabric.api.surface.BrowserSurface;
import io.github.trethore.graphene.fabric.api.surface.BrowserSurfaceInputAdapter;
import io.github.trethore.graphene.fabric.internal.input.GrapheneClickCounter;
import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenBridge;
import io.github.trethore.graphene.fabric.internal.util.MinecraftReferences;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("unused")
public class GrapheneWebViewWidget extends AbstractWidget implements AutoCloseable {
  private final Screen screen;
  private final BrowserSurface surface;
  private final BrowserSurfaceInputAdapter inputAdapter;
  private final GrapheneClickCounter clickCounter = new GrapheneClickCounter();
  private boolean closed;

  public GrapheneWebViewWidget(
      GrapheneContext context,
      Screen screen,
      int x,
      int y,
      int width,
      int height,
      Component message,
      String url) {
    this(
        screen,
        x,
        y,
        width,
        height,
        message,
        BrowserSurface.builder(context).url(url).size(width, height).build());
  }

  public GrapheneWebViewWidget(
      Screen screen,
      int x,
      int y,
      int width,
      int height,
      Component message,
      BrowserSurface surface) {
    super(x, y, width, height, message);
    this.screen = Objects.requireNonNull(screen, "screen");
    this.surface = Objects.requireNonNull(surface, "surface");
    this.inputAdapter = new BrowserSurfaceInputAdapter(surface);
    GLFW.glfwSetInputMode(
        MinecraftReferences.windowHandle(), GLFW.GLFW_LOCK_KEY_MODS, GLFW.GLFW_TRUE);
    requireScreenBridge(screen).graphene$addWebViewWidget(this);
    surface.resize(width, height);
  }

  public BrowserSurface surface() {
    return surface;
  }

  public GrapheneBridge bridge() {
    return surface.browser().bridge();
  }

  public String currentUrl() {
    return surface.browser().currentUrl();
  }

  public boolean canGoBack() {
    return surface.browser().canGoBack();
  }

  public boolean canGoForward() {
    return surface.browser().canGoForward();
  }

  public void navigate(String url) {
    surface.browser().navigate(url);
  }

  public void goBack() {
    surface.browser().goBack();
  }

  public void goForward() {
    surface.browser().goForward();
  }

  public void reload() {
    surface.browser().reload();
  }

  @Override
  protected void renderWidget(
      @NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    if (isMouseOver(mouseX, mouseY)) {
      inputAdapter.mouseMoved(mouseX, mouseY, getX(), getY(), getWidth(), getHeight(), 0);
    }
    surface.render(graphics, getX(), getY(), getWidth(), getHeight());
    if (isMouseOver(mouseX, mouseY)) {
      graphics.requestCursor(cursor());
    }
  }

  @Override
  protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
    // Web page accessibility is handled by the embedded browser.
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (!isMouseOver(event.x(), event.y())) {
      return false;
    }
    screen.setFocused(this);
    int clickCount = clickCounter.registerClick(event.button(), doubleClick, Util.getMillis());
    inputAdapter.mouseButton(
        event.x(),
        event.y(),
        getX(),
        getY(),
        getWidth(),
        getHeight(),
        event.button(),
        true,
        clickCount,
        event.modifiers());
    return true;
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    inputAdapter.mouseButton(
        event.x(),
        event.y(),
        getX(),
        getY(),
        getWidth(),
        getHeight(),
        event.button(),
        false,
        clickCounter.current(event.button()),
        event.modifiers());
    return true;
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
    inputAdapter.mouseDragged(
        event.x(), event.y(), getX(), getY(), getWidth(), getHeight(), event.modifiers());
    return true;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
    if (!isMouseOver(mouseX, mouseY)) {
      return false;
    }
    int modifiers = MinecraftReferences.hasControlDown() ? GLFW.GLFW_MOD_CONTROL : 0;
    inputAdapter.mouseScrolled(
        mouseX, mouseY, getX(), getY(), getWidth(), getHeight(), horizontal, vertical, modifiers);
    return true;
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    inputAdapter.key(event.key(), event.scancode(), true, event.modifiers());
    return true;
  }

  @Override
  public boolean keyReleased(KeyEvent event) {
    inputAdapter.key(event.key(), event.scancode(), false, event.modifiers());
    return true;
  }

  @Override
  public boolean charTyped(CharacterEvent event) {
    inputAdapter.text(event.codepointAsString(), event.modifiers());
    return true;
  }

  @Override
  public void setFocused(boolean focused) {
    super.setFocused(focused);
    inputAdapter.setFocused(focused);
  }

  @Override
  public void setSize(int width, int height) {
    super.setSize(width, height);
    surface.resize(width, height);
  }

  public void handleScreenResize() {
    surface.resize(getWidth(), getHeight());
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    requireScreenBridge(screen).graphene$removeWebViewWidget(this);
    inputAdapter.close();
    surface.close();
  }

  private static GrapheneScreenBridge requireScreenBridge(Screen screen) {
    if (screen instanceof GrapheneScreenBridge bridge) {
      return bridge;
    }
    throw new IllegalStateException(
        "Screen does not implement GrapheneScreenBridge: " + screen.getClass().getName());
  }

  private CursorType cursor() {
    return switch (surface.browser().requestedCursor()) {
      case CROSSHAIR -> CursorTypes.CROSSHAIR;
      case TEXT -> CursorTypes.IBEAM;
      case HAND -> CursorTypes.POINTING_HAND;
      case NOT_ALLOWED -> CursorTypes.NOT_ALLOWED;
      case RESIZE_HORIZONTAL -> CursorTypes.RESIZE_EW;
      case RESIZE_VERTICAL -> CursorTypes.RESIZE_NS;
      case RESIZE_ALL -> CursorTypes.RESIZE_ALL;
      case ARROW -> CursorTypes.ARROW;
    };
  }
}
