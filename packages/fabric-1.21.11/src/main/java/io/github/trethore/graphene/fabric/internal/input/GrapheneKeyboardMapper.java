package io.github.trethore.graphene.fabric.internal.input;

import io.github.trethore.graphene.api.browser.input.BrowserKey;
import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserKeyLocation;
import io.github.trethore.graphene.api.browser.input.BrowserKeyPlatform;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserRawKeyMetadata;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

public final class GrapheneKeyboardMapper {
  private static final BrowserKey[] DIGIT_KEYS = {
    BrowserKey.DIGIT_0,
    BrowserKey.DIGIT_1,
    BrowserKey.DIGIT_2,
    BrowserKey.DIGIT_3,
    BrowserKey.DIGIT_4,
    BrowserKey.DIGIT_5,
    BrowserKey.DIGIT_6,
    BrowserKey.DIGIT_7,
    BrowserKey.DIGIT_8,
    BrowserKey.DIGIT_9
  };
  private static final BrowserKey[] LETTER_KEYS = {
    BrowserKey.KEY_A,
    BrowserKey.KEY_B,
    BrowserKey.KEY_C,
    BrowserKey.KEY_D,
    BrowserKey.KEY_E,
    BrowserKey.KEY_F,
    BrowserKey.KEY_G,
    BrowserKey.KEY_H,
    BrowserKey.KEY_I,
    BrowserKey.KEY_J,
    BrowserKey.KEY_K,
    BrowserKey.KEY_L,
    BrowserKey.KEY_M,
    BrowserKey.KEY_N,
    BrowserKey.KEY_O,
    BrowserKey.KEY_P,
    BrowserKey.KEY_Q,
    BrowserKey.KEY_R,
    BrowserKey.KEY_S,
    BrowserKey.KEY_T,
    BrowserKey.KEY_U,
    BrowserKey.KEY_V,
    BrowserKey.KEY_W,
    BrowserKey.KEY_X,
    BrowserKey.KEY_Y,
    BrowserKey.KEY_Z
  };
  private static final BrowserKey[] FUNCTION_KEYS = {
    BrowserKey.F1,
    BrowserKey.F2,
    BrowserKey.F3,
    BrowserKey.F4,
    BrowserKey.F5,
    BrowserKey.F6,
    BrowserKey.F7,
    BrowserKey.F8,
    BrowserKey.F9,
    BrowserKey.F10,
    BrowserKey.F11,
    BrowserKey.F12,
    BrowserKey.F13,
    BrowserKey.F14,
    BrowserKey.F15,
    BrowserKey.F16,
    BrowserKey.F17,
    BrowserKey.F18,
    BrowserKey.F19,
    BrowserKey.F20,
    BrowserKey.F21,
    BrowserKey.F22,
    BrowserKey.F23,
    BrowserKey.F24,
    BrowserKey.F25
  };

  private GrapheneKeyboardMapper() {}

  public static BrowserKeyInput map(
      int keyCode,
      int scanCode,
      boolean pressed,
      int glfwModifiers,
      Set<BrowserModifier> modifiers) {
    BrowserKey key = key(keyCode);
    BrowserKeyLocation location = location(keyCode);
    BrowserRawKeyMetadata rawMetadata =
        new BrowserRawKeyMetadata(
            BrowserKeyPlatform.current(),
            Math.max(scanCode, 0),
            layoutCodePoint(keyCode, scanCode, glfwModifiers));
    return new BrowserKeyInput(
        pressed ? BrowserKeyAction.PRESS : BrowserKeyAction.RELEASE,
        key,
        location,
        modifiers,
        rawMetadata);
  }

  static BrowserKey key(int keyCode) {
    if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
      return DIGIT_KEYS[keyCode - GLFW.GLFW_KEY_0];
    }
    if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
      return LETTER_KEYS[keyCode - GLFW.GLFW_KEY_A];
    }
    if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
      return FUNCTION_KEYS[keyCode - GLFW.GLFW_KEY_F1];
    }
    if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
      return DIGIT_KEYS[keyCode - GLFW.GLFW_KEY_KP_0];
    }
    return symbolKey(keyCode);
  }

  private static BrowserKey symbolKey(int keyCode) {
    return switch (keyCode) {
      case GLFW.GLFW_KEY_SPACE -> BrowserKey.SPACE;
      case GLFW.GLFW_KEY_APOSTROPHE -> BrowserKey.QUOTE;
      case GLFW.GLFW_KEY_COMMA -> BrowserKey.COMMA;
      case GLFW.GLFW_KEY_MINUS -> BrowserKey.MINUS;
      case GLFW.GLFW_KEY_PERIOD -> BrowserKey.PERIOD;
      case GLFW.GLFW_KEY_SLASH -> BrowserKey.SLASH;
      case GLFW.GLFW_KEY_SEMICOLON -> BrowserKey.SEMICOLON;
      case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_EQUAL -> BrowserKey.EQUAL;
      case GLFW.GLFW_KEY_LEFT_BRACKET -> BrowserKey.BRACKET_LEFT;
      case GLFW.GLFW_KEY_BACKSLASH -> BrowserKey.BACKSLASH;
      case GLFW.GLFW_KEY_RIGHT_BRACKET -> BrowserKey.BRACKET_RIGHT;
      case GLFW.GLFW_KEY_GRAVE_ACCENT -> BrowserKey.BACKQUOTE;
      case GLFW.GLFW_KEY_WORLD_1 -> BrowserKey.INTERNATIONAL_1;
      case GLFW.GLFW_KEY_WORLD_2 -> BrowserKey.INTERNATIONAL_2;
      default -> specialKey(keyCode);
    };
  }

  private static BrowserKey specialKey(int keyCode) {
    return switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE -> BrowserKey.ESCAPE;
      case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> BrowserKey.ENTER;
      case GLFW.GLFW_KEY_TAB -> BrowserKey.TAB;
      case GLFW.GLFW_KEY_BACKSPACE -> BrowserKey.BACKSPACE;
      case GLFW.GLFW_KEY_INSERT -> BrowserKey.INSERT;
      case GLFW.GLFW_KEY_DELETE -> BrowserKey.DELETE;
      case GLFW.GLFW_KEY_RIGHT -> BrowserKey.ARROW_RIGHT;
      case GLFW.GLFW_KEY_LEFT -> BrowserKey.ARROW_LEFT;
      case GLFW.GLFW_KEY_DOWN -> BrowserKey.ARROW_DOWN;
      case GLFW.GLFW_KEY_UP -> BrowserKey.ARROW_UP;
      case GLFW.GLFW_KEY_PAGE_UP -> BrowserKey.PAGE_UP;
      case GLFW.GLFW_KEY_PAGE_DOWN -> BrowserKey.PAGE_DOWN;
      case GLFW.GLFW_KEY_HOME -> BrowserKey.HOME;
      case GLFW.GLFW_KEY_END -> BrowserKey.END;
      case GLFW.GLFW_KEY_CAPS_LOCK -> BrowserKey.CAPS_LOCK;
      case GLFW.GLFW_KEY_SCROLL_LOCK -> BrowserKey.SCROLL_LOCK;
      case GLFW.GLFW_KEY_NUM_LOCK -> BrowserKey.NUM_LOCK;
      case GLFW.GLFW_KEY_PRINT_SCREEN -> BrowserKey.PRINT_SCREEN;
      case GLFW.GLFW_KEY_PAUSE -> BrowserKey.PAUSE;
      default -> keypadOrModifierKey(keyCode);
    };
  }

  private static BrowserKey keypadOrModifierKey(int keyCode) {
    return switch (keyCode) {
      case GLFW.GLFW_KEY_KP_DECIMAL -> BrowserKey.DECIMAL;
      case GLFW.GLFW_KEY_KP_DIVIDE -> BrowserKey.DIVIDE;
      case GLFW.GLFW_KEY_KP_MULTIPLY -> BrowserKey.MULTIPLY;
      case GLFW.GLFW_KEY_KP_SUBTRACT -> BrowserKey.SUBTRACT;
      case GLFW.GLFW_KEY_KP_ADD -> BrowserKey.ADD;
      case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> BrowserKey.SHIFT;
      case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> BrowserKey.CONTROL;
      case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> BrowserKey.ALT;
      case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> BrowserKey.META;
      case GLFW.GLFW_KEY_MENU -> BrowserKey.CONTEXT_MENU;
      default -> BrowserKey.UNKNOWN;
    };
  }

  static BrowserKeyLocation location(int keyCode) {
    if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_EQUAL) {
      return BrowserKeyLocation.NUMPAD;
    }
    return switch (keyCode) {
      case GLFW.GLFW_KEY_LEFT_SHIFT,
          GLFW.GLFW_KEY_LEFT_CONTROL,
          GLFW.GLFW_KEY_LEFT_ALT,
          GLFW.GLFW_KEY_LEFT_SUPER ->
          BrowserKeyLocation.LEFT;
      case GLFW.GLFW_KEY_RIGHT_SHIFT,
          GLFW.GLFW_KEY_RIGHT_CONTROL,
          GLFW.GLFW_KEY_RIGHT_ALT,
          GLFW.GLFW_KEY_RIGHT_SUPER ->
          BrowserKeyLocation.RIGHT;
      default -> BrowserKeyLocation.STANDARD;
    };
  }

  private static int layoutCodePoint(int keyCode, int scanCode, int modifiers) {
    if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_EQUAL) {
      return keypadCodePoint(keyCode, (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0);
    }
    String name = scanCode > 0 ? GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scanCode) : null;
    if (name == null || name.isBlank()) {
      name = GLFW.glfwGetKeyName(keyCode, scanCode);
    }
    if (name == null || name.isBlank()) {
      return 0;
    }
    return name.codePointAt(0);
  }

  private static int keypadCodePoint(int keyCode, boolean numLock) {
    if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
      return numLock ? '0' + keyCode - GLFW.GLFW_KEY_KP_0 : 0;
    }
    return switch (keyCode) {
      case GLFW.GLFW_KEY_KP_DECIMAL -> numLock ? '.' : 0;
      case GLFW.GLFW_KEY_KP_DIVIDE -> '/';
      case GLFW.GLFW_KEY_KP_MULTIPLY -> '*';
      case GLFW.GLFW_KEY_KP_SUBTRACT -> '-';
      case GLFW.GLFW_KEY_KP_ADD -> '+';
      case GLFW.GLFW_KEY_KP_EQUAL -> '=';
      case GLFW.GLFW_KEY_KP_ENTER -> '\r';
      default -> 0;
    };
  }
}
