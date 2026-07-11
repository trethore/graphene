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
  private static final long SYNTHETIC_DUPLICATE_WINDOW_MILLIS = 250;

  private final BrowserSurface surface;
  private char pendingSyntheticCharacter;
  private long pendingSyntheticTimestamp;
  private boolean rightAltPressed;
  private BrowserPointerButton pressedButton = BrowserPointerButton.NONE;

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
    BrowserPointerButton browserButton = pointerButton(button);
    if (pressed) {
      pressedButton = browserButton;
    } else if (pressedButton == browserButton) {
      pressedButton = BrowserPointerButton.NONE;
    }
    sendPointer(
        pressed ? BrowserPointerAction.PRESS : BrowserPointerAction.RELEASE,
        browserButton,
        mouseX,
        mouseY,
        surfaceX,
        surfaceY,
        renderedWidth,
        renderedHeight,
        clickCount,
        modifiers);
  }

  public void mouseDragged(
      double mouseX,
      double mouseY,
      int surfaceX,
      int surfaceY,
      int renderedWidth,
      int renderedHeight,
      int modifiers) {
    sendPointer(
        BrowserPointerAction.DRAG,
        pressedButton,
        mouseX,
        mouseY,
        surfaceX,
        surfaceY,
        renderedWidth,
        renderedHeight,
        0,
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
    if (keyCode == GLFW.GLFW_KEY_RIGHT_ALT) {
      rightAltPressed = pressed;
    }
    surface
        .browser()
        .sendKeyInput(
            GrapheneKeyboardMapper.map(
                keyCode, scanCode, pressed, modifiers, modifiers(modifiers)));
    if (pressed) {
      char syntheticCharacter = syntheticCharacter(keyCode, modifiers);
      if (syntheticCharacter != 0) {
        sendText(syntheticCharacter, modifiers);
        pendingSyntheticCharacter = syntheticCharacter;
        pendingSyntheticTimestamp = System.currentTimeMillis();
      }
    }
  }

  public void text(char character, int modifiers) {
    char normalizedCharacter = normalizeTextCharacter(character);
    if (normalizedCharacter == 0 || isSyntheticDuplicate(normalizedCharacter)) {
      return;
    }
    sendText(normalizedCharacter, modifiers);
  }

  private void sendText(char character, int modifiers) {
    Set<BrowserModifier> browserModifiers = modifiers(modifiers);
    if (rightAltPressed
        && browserModifiers.contains(BrowserModifier.ALT)
        && browserModifiers.contains(BrowserModifier.CONTROL)) {
      EnumSet<BrowserModifier> sanitized = EnumSet.copyOf(browserModifiers);
      sanitized.remove(BrowserModifier.ALT);
      sanitized.remove(BrowserModifier.CONTROL);
      browserModifiers = Set.copyOf(sanitized);
    }
    surface.browser().sendTextInput(new BrowserTextInput(character, browserModifiers));
  }

  private boolean isSyntheticDuplicate(char character) {
    if (pendingSyntheticCharacter == 0) {
      return false;
    }
    boolean duplicate =
        pendingSyntheticCharacter == character
            && System.currentTimeMillis() - pendingSyntheticTimestamp
                <= SYNTHETIC_DUPLICATE_WINDOW_MILLIS;
    pendingSyntheticCharacter = 0;
    pendingSyntheticTimestamp = 0;
    return duplicate;
  }

  private static char syntheticCharacter(int keyCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
      return '\b';
    }
    if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
      return '\r';
    }
    if (keyCode >= GLFW.GLFW_KEY_KP_0
        && keyCode <= GLFW.GLFW_KEY_KP_9
        && (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0) {
      return (char) ('0' + keyCode - GLFW.GLFW_KEY_KP_0);
    }
    return switch (keyCode) {
      case GLFW.GLFW_KEY_KP_DECIMAL -> (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0 ? '.' : 0;
      case GLFW.GLFW_KEY_KP_DIVIDE -> '/';
      case GLFW.GLFW_KEY_KP_MULTIPLY -> '*';
      case GLFW.GLFW_KEY_KP_SUBTRACT -> '-';
      case GLFW.GLFW_KEY_KP_ADD -> '+';
      case GLFW.GLFW_KEY_KP_EQUAL -> '=';
      default -> 0;
    };
  }

  private static char normalizeTextCharacter(char character) {
    if (character == 0x7F) {
      return '\b';
    }
    if (character == '\n') {
      return '\r';
    }
    if ((character >= '\uF700' && character <= '\uF8FF')
        || (Character.isISOControl(character)
            && character != '\b'
            && character != '\t'
            && character != '\r')) {
      return 0;
    }
    return character;
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
