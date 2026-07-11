package io.github.trethore.graphene.fabric.internal.input;

import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

public final class GrapheneKeyboardMapper {
  private static final boolean WINDOWS = osName().contains("win");
  private static final boolean MAC = osName().contains("mac");

  private GrapheneKeyboardMapper() {}

  public static BrowserKeyInput map(
      int keyCode,
      int scanCode,
      boolean pressed,
      int glfwModifiers,
      Set<BrowserModifier> modifiers) {
    int resolvedScanCode = resolveScanCode(keyCode, scanCode);
    char layoutCharacter = layoutCharacter(keyCode, resolvedScanCode, glfwModifiers);
    int domKeyCode = windowsVirtualKey(keyCode, layoutCharacter);
    int nativeKeyCode = nativeKeyCode(keyCode, resolvedScanCode, domKeyCode, pressed);
    EnumSet<BrowserModifier> resolvedModifiers =
        modifiers.isEmpty() ? EnumSet.noneOf(BrowserModifier.class) : EnumSet.copyOf(modifiers);
    if (isKeypad(keyCode)) {
      resolvedModifiers.add(BrowserModifier.KEYPAD);
    }
    if (isLeftModifier(keyCode)) {
      resolvedModifiers.add(BrowserModifier.LEFT);
    } else if (isRightModifier(keyCode)) {
      resolvedModifiers.add(BrowserModifier.RIGHT);
    }
    boolean systemKey =
        MAC ? (glfwModifiers & GLFW.GLFW_MOD_SUPER) != 0 : (glfwModifiers & GLFW.GLFW_MOD_ALT) != 0;
    char unmodifiedCharacter = rawUnmodifiedCharacter(keyCode, layoutCharacter);
    char character = rawCharacter(keyCode, unmodifiedCharacter, glfwModifiers);
    return new BrowserKeyInput(
        pressed ? BrowserKeyAction.PRESS : BrowserKeyAction.RELEASE,
        domKeyCode,
        nativeKeyCode,
        WINDOWS ? resolvedScanCode & 0xFFL : resolvedScanCode,
        systemKey,
        character,
        unmodifiedCharacter,
        resolvedModifiers);
  }

  static int windowsVirtualKey(int keyCode, char character) {
    if (character >= 'a' && character <= 'z') {
      return Character.toUpperCase(character);
    }
    if (character >= 'A' && character <= 'Z') {
      return character;
    }
    return switch (keyCode) {
      case GLFW.GLFW_KEY_BACKSPACE -> 0x08;
      case GLFW.GLFW_KEY_TAB -> 0x09;
      case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 0x0D;
      case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> 0x10;
      case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> 0x11;
      case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> 0x12;
      case GLFW.GLFW_KEY_ESCAPE -> 0x1B;
      case GLFW.GLFW_KEY_SPACE -> 0x20;
      case GLFW.GLFW_KEY_PAGE_UP -> 0x21;
      case GLFW.GLFW_KEY_PAGE_DOWN -> 0x22;
      case GLFW.GLFW_KEY_END -> 0x23;
      case GLFW.GLFW_KEY_HOME -> 0x24;
      case GLFW.GLFW_KEY_LEFT -> 0x25;
      case GLFW.GLFW_KEY_UP -> 0x26;
      case GLFW.GLFW_KEY_RIGHT -> 0x27;
      case GLFW.GLFW_KEY_DOWN -> 0x28;
      case GLFW.GLFW_KEY_PRINT_SCREEN -> 0x2C;
      case GLFW.GLFW_KEY_INSERT -> 0x2D;
      case GLFW.GLFW_KEY_DELETE -> 0x2E;
      case GLFW.GLFW_KEY_LEFT_SUPER -> 0x5B;
      case GLFW.GLFW_KEY_RIGHT_SUPER -> 0x5C;
      case GLFW.GLFW_KEY_KP_0,
          GLFW.GLFW_KEY_KP_1,
          GLFW.GLFW_KEY_KP_2,
          GLFW.GLFW_KEY_KP_3,
          GLFW.GLFW_KEY_KP_4,
          GLFW.GLFW_KEY_KP_5,
          GLFW.GLFW_KEY_KP_6,
          GLFW.GLFW_KEY_KP_7,
          GLFW.GLFW_KEY_KP_8,
          GLFW.GLFW_KEY_KP_9 ->
          0x60 + keyCode - GLFW.GLFW_KEY_KP_0;
      case GLFW.GLFW_KEY_F1,
          GLFW.GLFW_KEY_F2,
          GLFW.GLFW_KEY_F3,
          GLFW.GLFW_KEY_F4,
          GLFW.GLFW_KEY_F5,
          GLFW.GLFW_KEY_F6,
          GLFW.GLFW_KEY_F7,
          GLFW.GLFW_KEY_F8,
          GLFW.GLFW_KEY_F9,
          GLFW.GLFW_KEY_F10,
          GLFW.GLFW_KEY_F11,
          GLFW.GLFW_KEY_F12 ->
          0x70 + keyCode - GLFW.GLFW_KEY_F1;
      case GLFW.GLFW_KEY_NUM_LOCK -> 0x90;
      case GLFW.GLFW_KEY_SCROLL_LOCK -> 0x91;
      case GLFW.GLFW_KEY_SEMICOLON -> 0xBA;
      case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_EQUAL -> 0xBB;
      case GLFW.GLFW_KEY_COMMA -> 0xBC;
      case GLFW.GLFW_KEY_MINUS -> 0xBD;
      case GLFW.GLFW_KEY_PERIOD -> 0xBE;
      case GLFW.GLFW_KEY_SLASH -> 0xBF;
      case GLFW.GLFW_KEY_GRAVE_ACCENT -> 0xC0;
      case GLFW.GLFW_KEY_LEFT_BRACKET -> 0xDB;
      case GLFW.GLFW_KEY_BACKSLASH -> 0xDC;
      case GLFW.GLFW_KEY_RIGHT_BRACKET -> 0xDD;
      case GLFW.GLFW_KEY_APOSTROPHE -> 0xDE;
      default -> character;
    };
  }

  private static int nativeKeyCode(int keyCode, int scanCode, int domKeyCode, boolean pressed) {
    if (WINDOWS) {
      return scanCode <= 0
          ? 0
          : windowsNativeKeyCode(scanCode, isExtendedWindowsKey(keyCode), !pressed);
    }
    if (MAC) {
      return macKeyCode(keyCode, scanCode);
    }
    return switch (keyCode) {
      case GLFW.GLFW_KEY_BACKSPACE -> 0xFF08;
      case GLFW.GLFW_KEY_TAB -> 0xFF09;
      case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 0xFF0D;
      case GLFW.GLFW_KEY_ESCAPE -> 0xFF1B;
      case GLFW.GLFW_KEY_LEFT -> 0xFF51;
      case GLFW.GLFW_KEY_UP -> 0xFF52;
      case GLFW.GLFW_KEY_RIGHT -> 0xFF53;
      case GLFW.GLFW_KEY_DOWN -> 0xFF54;
      case GLFW.GLFW_KEY_DELETE -> 0xFFFF;
      default -> domKeyCode;
    };
  }

  private static int resolveScanCode(int keyCode, int scanCode) {
    if (!WINDOWS) {
      return Math.max(scanCode, 0);
    }
    int mapped =
        switch (keyCode) {
          case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> 29;
          case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 28;
          case GLFW.GLFW_KEY_HOME -> 71;
          case GLFW.GLFW_KEY_UP -> 72;
          case GLFW.GLFW_KEY_PAGE_UP -> 73;
          case GLFW.GLFW_KEY_LEFT -> 75;
          case GLFW.GLFW_KEY_RIGHT -> 77;
          case GLFW.GLFW_KEY_END -> 79;
          case GLFW.GLFW_KEY_DOWN -> 80;
          case GLFW.GLFW_KEY_PAGE_DOWN -> 81;
          case GLFW.GLFW_KEY_DELETE -> 83;
          default -> 0;
        };
    return mapped != 0 ? mapped : Math.max(scanCode, 0);
  }

  private static char layoutCharacter(int keyCode, int scanCode, int modifiers) {
    if (isKeypad(keyCode)) {
      return keypadCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0);
    }
    String name = scanCode > 0 ? GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scanCode) : null;
    if (name == null || name.isBlank()) {
      name = GLFW.glfwGetKeyName(keyCode, scanCode);
    }
    if (name == null || name.isBlank()) {
      return 0;
    }
    char character = name.charAt(0);
    return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? Character.toUpperCase(character) : character;
  }

  private static char rawUnmodifiedCharacter(int keyCode, char layoutCharacter) {
    if (MAC) {
      return macRawCharacter(keyCode, layoutCharacter);
    }
    if (!WINDOWS) {
      return switch (keyCode) {
        case GLFW.GLFW_KEY_BACKSPACE -> (char) 0xFF08;
        case GLFW.GLFW_KEY_TAB -> (char) 0xFF09;
        case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> '\r';
        case GLFW.GLFW_KEY_ESCAPE -> (char) 0xFF1B;
        case GLFW.GLFW_KEY_LEFT -> (char) 0xFF51;
        case GLFW.GLFW_KEY_UP -> (char) 0xFF52;
        case GLFW.GLFW_KEY_RIGHT -> (char) 0xFF53;
        case GLFW.GLFW_KEY_DOWN -> (char) 0xFF54;
        case GLFW.GLFW_KEY_DELETE -> (char) 0xFFFF;
        default -> layoutCharacter;
      };
    }
    return 0;
  }

  private static char rawCharacter(int keyCode, char unmodifiedCharacter, int modifiers) {
    if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
      return unmodifiedCharacter;
    }
    if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
      return (char) (keyCode - GLFW.GLFW_KEY_A + 1);
    }
    return switch (keyCode) {
      case GLFW.GLFW_KEY_LEFT_BRACKET -> 0x1B;
      case GLFW.GLFW_KEY_BACKSLASH -> 0x1C;
      case GLFW.GLFW_KEY_RIGHT_BRACKET -> 0x1D;
      case GLFW.GLFW_KEY_6 -> (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 0x1E : unmodifiedCharacter;
      case GLFW.GLFW_KEY_MINUS ->
          (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 0x1F : unmodifiedCharacter;
      case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> '\n';
      default -> unmodifiedCharacter;
    };
  }

  private static char keypadCharacter(int keyCode, boolean numLock) {
    if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
      return numLock ? (char) ('0' + keyCode - GLFW.GLFW_KEY_KP_0) : 0;
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

  private static char macRawCharacter(int keyCode, char fallback) {
    return switch (keyCode) {
      case GLFW.GLFW_KEY_BACKSPACE -> 0x7F;
      case GLFW.GLFW_KEY_LEFT -> '\uF702';
      case GLFW.GLFW_KEY_RIGHT -> '\uF703';
      case GLFW.GLFW_KEY_UP -> '\uF700';
      case GLFW.GLFW_KEY_DOWN -> '\uF701';
      case GLFW.GLFW_KEY_INSERT -> '\uF727';
      case GLFW.GLFW_KEY_DELETE -> '\uF728';
      case GLFW.GLFW_KEY_HOME -> '\uF729';
      case GLFW.GLFW_KEY_END -> '\uF72B';
      case GLFW.GLFW_KEY_PAGE_UP -> '\uF72C';
      case GLFW.GLFW_KEY_PAGE_DOWN -> '\uF72D';
      default -> fallback;
    };
  }

  private static int macKeyCode(int keyCode, int fallback) {
    if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
      int[] codes = {
        0, 11, 8, 2, 14, 3, 5, 4, 34, 38, 40, 37, 46, 45, 31, 35, 12, 15, 1, 17, 32, 9, 13, 7, 16, 6
      };
      return codes[keyCode - GLFW.GLFW_KEY_A];
    }
    return switch (keyCode) {
      case GLFW.GLFW_KEY_ENTER -> 0x24;
      case GLFW.GLFW_KEY_TAB -> 0x30;
      case GLFW.GLFW_KEY_SPACE -> 0x31;
      case GLFW.GLFW_KEY_BACKSPACE -> 0x33;
      case GLFW.GLFW_KEY_ESCAPE -> 0x35;
      case GLFW.GLFW_KEY_LEFT -> 0x7B;
      case GLFW.GLFW_KEY_RIGHT -> 0x7C;
      case GLFW.GLFW_KEY_DOWN -> 0x7D;
      case GLFW.GLFW_KEY_UP -> 0x7E;
      default -> Math.max(fallback, 0);
    };
  }

  private static boolean isKeypad(int keyCode) {
    return keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_EQUAL;
  }

  private static boolean isLeftModifier(int keyCode) {
    return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT
        || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL
        || keyCode == GLFW.GLFW_KEY_LEFT_ALT
        || keyCode == GLFW.GLFW_KEY_LEFT_SUPER;
  }

  private static boolean isRightModifier(int keyCode) {
    return keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
        || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
        || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
        || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
  }

  private static boolean isExtendedWindowsKey(int keyCode) {
    return isRightModifier(keyCode)
        || keyCode == GLFW.GLFW_KEY_INSERT
        || keyCode == GLFW.GLFW_KEY_DELETE
        || keyCode == GLFW.GLFW_KEY_HOME
        || keyCode == GLFW.GLFW_KEY_END
        || keyCode == GLFW.GLFW_KEY_PAGE_UP
        || keyCode == GLFW.GLFW_KEY_PAGE_DOWN
        || keyCode == GLFW.GLFW_KEY_UP
        || keyCode == GLFW.GLFW_KEY_DOWN
        || keyCode == GLFW.GLFW_KEY_LEFT
        || keyCode == GLFW.GLFW_KEY_RIGHT
        || keyCode == GLFW.GLFW_KEY_KP_ENTER
        || keyCode == GLFW.GLFW_KEY_KP_DIVIDE
        || keyCode == GLFW.GLFW_KEY_NUM_LOCK
        || keyCode == GLFW.GLFW_KEY_PRINT_SCREEN;
  }

  private static int windowsNativeKeyCode(int scanCode, boolean extended, boolean keyUp) {
    int nativeCode = (scanCode << 16) | 1;
    if (extended) {
      nativeCode |= 1 << 24;
    }
    if (keyUp) {
      nativeCode |= 0xC0000000;
    }
    return nativeCode;
  }

  private static String osName() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  }
}
