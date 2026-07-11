package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserPointerAction;
import io.github.trethore.graphene.api.browser.input.BrowserPointerButton;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserScrollInput;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import io.github.trethore.graphene.fabric.internal.input.GrapheneKeyboardMapper;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("unused")
public final class BrowserSurfaceInputAdapter {
  private static final int SCROLL_DELTA = 120;

  private final BrowserSurface surface;

  public BrowserSurfaceInputAdapter(BrowserSurface surface) {
    this.surface = Objects.requireNonNull(surface, "surface");
  }

  public void setFocused(boolean focused) {
    surface.browser().setFocused(focused);
  }

  public void mouseMoved(
      double mouseX,
      double mouseY,
      int surfaceX,
      int surfaceY,
      int renderedWidth,
      int renderedHeight,
      int modifiers) {
    sendPointer(
        BrowserPointerAction.MOVE,
        BrowserPointerButton.NONE,
        mouseX,
        mouseY,
        surfaceX,
        surfaceY,
        renderedWidth,
        renderedHeight,
        0,
        modifiers);
  }

  public void mouseButton(
      double mouseX,
      double mouseY,
      int surfaceX,
      int surfaceY,
      int renderedWidth,
      int renderedHeight,
      int button,
      boolean pressed,
      int clickCount,
      int modifiers) {
    sendPointer(
        pressed ? BrowserPointerAction.PRESS : BrowserPointerAction.RELEASE,
        pointerButton(button),
        mouseX,
        mouseY,
        surfaceX,
        surfaceY,
        renderedWidth,
        renderedHeight,
        clickCount,
        modifiers);
  }

  public void mouseScrolled(
      double mouseX,
      double mouseY,
      int surfaceX,
      int surfaceY,
      int renderedWidth,
      int renderedHeight,
      double horizontal,
      double vertical,
      int modifiers) {
    int browserX = surface.toBrowserX(mouseX - surfaceX, renderedWidth);
    int browserY = surface.toBrowserY(mouseY - surfaceY, renderedHeight);
    surface
        .browser()
        .sendScrollInput(
            new BrowserScrollInput(
                browserX,
                browserY,
                (int) Math.round(horizontal * SCROLL_DELTA),
                (int) Math.round(vertical * SCROLL_DELTA),
                modifiers(modifiers)));
  }

  public void key(int keyCode, int scanCode, boolean pressed, int modifiers) {
    surface
        .browser()
        .sendKeyInput(
            GrapheneKeyboardMapper.map(
                keyCode, scanCode, pressed, modifiers, modifiers(modifiers)));
  }

  public void text(char character, int modifiers) {
    surface.browser().sendTextInput(new BrowserTextInput(character, modifiers(modifiers)));
  }

  private void sendPointer(
      BrowserPointerAction action,
      BrowserPointerButton button,
      double mouseX,
      double mouseY,
      int surfaceX,
      int surfaceY,
      int renderedWidth,
      int renderedHeight,
      int clickCount,
      int modifiers) {
    BrowserSession browser = surface.browser();
    browser.sendPointerInput(
        new BrowserPointerInput(
            action,
            surface.toBrowserX(mouseX - surfaceX, renderedWidth),
            surface.toBrowserY(mouseY - surfaceY, renderedHeight),
            button,
            clickCount,
            modifiers(modifiers)));
  }

  private static BrowserPointerButton pointerButton(int button) {
    return switch (button) {
      case GLFW.GLFW_MOUSE_BUTTON_LEFT -> BrowserPointerButton.LEFT;
      case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> BrowserPointerButton.MIDDLE;
      case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> BrowserPointerButton.RIGHT;
      default -> BrowserPointerButton.NONE;
    };
  }

  private static Set<BrowserModifier> modifiers(int modifiers) {
    EnumSet<BrowserModifier> result = EnumSet.noneOf(BrowserModifier.class);
    if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
      result.add(BrowserModifier.SHIFT);
    }
    if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
      result.add(BrowserModifier.CONTROL);
    }
    if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
      result.add(BrowserModifier.ALT);
    }
    if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0) {
      result.add(BrowserModifier.META);
    }
    if ((modifiers & GLFW.GLFW_MOD_CAPS_LOCK) != 0) {
      result.add(BrowserModifier.CAPS_LOCK);
    }
    if ((modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0) {
      result.add(BrowserModifier.NUM_LOCK);
    }
    return Set.copyOf(result);
  }
}
