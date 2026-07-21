package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.input.BrowserKey;
import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserKeyLocation;
import io.github.trethore.graphene.api.browser.input.BrowserKeyPlatform;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserRawKeyMetadata;
import org.cef.input.CefKeyEvent;

final class GrapheneCefKeyMapper {
  private GrapheneCefKeyMapper() {}

  static ResolvedKey resolve(BrowserKeyInput input) {
    BrowserRawKeyMetadata rawMetadata = input.rawMetadata().orElse(null);
    BrowserKeyPlatform platform =
        rawMetadata == null ? BrowserKeyPlatform.current() : rawMetadata.platform();
    long suppliedScanCode = rawMetadata == null ? 0 : rawMetadata.scanCode();
    long scanCode = resolveScanCode(input.key(), input.location(), platform, suppliedScanCode);
    char layoutCharacter = layoutCharacter(input, rawMetadata);
    int windowsKeyCode = windowsVirtualKey(input, layoutCharacter);
    int nativeKeyCode =
        nativeKeyCode(input.key(), input.location(), input.action(), platform, scanCode);
    char unmodifiedCharacter = unmodifiedCharacter(input.key(), platform, layoutCharacter);
    char character =
        character(
            input.key(),
            unmodifiedCharacter,
            input.modifiers().contains(BrowserModifier.CONTROL),
            input.modifiers().contains(BrowserModifier.SHIFT));
    boolean systemKey =
        platform == BrowserKeyPlatform.WINDOWS && input.modifiers().contains(BrowserModifier.ALT);
    return new ResolvedKey(
        platform,
        windowsKeyCode,
        nativeKeyCode,
        systemKey,
        character,
        unmodifiedCharacter,
        scanCode);
  }

  private static char layoutCharacter(BrowserKeyInput input, BrowserRawKeyMetadata rawMetadata) {
    int codePoint = rawMetadata == null ? 0 : rawMetadata.layoutCodePoint();
    if (codePoint == 0) {
      codePoint = defaultLayoutCodePoint(input);
    }
    if (codePoint == 0 || !Character.isBmpCodePoint(codePoint)) {
      return 0;
    }
    char character = (char) codePoint;
    return input.modifiers().contains(BrowserModifier.SHIFT)
        ? Character.toUpperCase(character)
        : character;
  }

  private static int defaultLayoutCodePoint(BrowserKeyInput input) {
    BrowserKey key = input.key();
    int letter = letter(key);
    if (letter >= 0) {
      return 'a' + letter;
    }
    int digit = digit(key);
    if (digit >= 0) {
      return input.location() != BrowserKeyLocation.NUMPAD
              || input.modifiers().contains(BrowserModifier.NUM_LOCK)
          ? '0' + digit
          : 0;
    }
    return switch (key) {
      case BACKQUOTE -> '`';
      case BACKSLASH -> '\\';
      case BRACKET_LEFT -> '[';
      case BRACKET_RIGHT -> ']';
      case COMMA -> ',';
      case EQUAL -> '=';
      case MINUS, SUBTRACT -> '-';
      case PERIOD -> '.';
      case DECIMAL -> input.modifiers().contains(BrowserModifier.NUM_LOCK) ? '.' : 0;
      case QUOTE -> '\'';
      case SEMICOLON -> ';';
      case SLASH, DIVIDE -> '/';
      case ADD -> '+';
      case MULTIPLY -> '*';
      case SPACE -> ' ';
      case ENTER -> '\r';
      default -> 0;
    };
  }

  @SuppressWarnings("java:S1479")
  private static int windowsVirtualKey(BrowserKeyInput input, char layoutCharacter) {
    BrowserKey key = input.key();
    BrowserKeyLocation location = input.location();
    int digit = digit(key);
    if (digit >= 0) {
      if (location == BrowserKeyLocation.NUMPAD) {
        return keypadDigitVirtualKey(digit, input.modifiers().contains(BrowserModifier.NUM_LOCK));
      }
      return '0' + digit;
    }
    if (layoutCharacter >= 'a' && layoutCharacter <= 'z') {
      return Character.toUpperCase(layoutCharacter);
    }
    if (layoutCharacter >= 'A' && layoutCharacter <= 'Z') {
      return layoutCharacter;
    }
    int letter = letter(key);
    if (letter >= 0) {
      return 'A' + letter;
    }
    return switch (key) {
      case BACKSPACE -> 0x08;
      case TAB -> 0x09;
      case ENTER -> 0x0D;
      case SHIFT -> 0x10;
      case CONTROL -> 0x11;
      case ALT -> 0x12;
      case PAUSE -> 0x13;
      case CAPS_LOCK -> 0x14;
      case ESCAPE -> 0x1B;
      case SPACE -> 0x20;
      case PAGE_UP -> 0x21;
      case PAGE_DOWN -> 0x22;
      case END -> 0x23;
      case HOME -> 0x24;
      case ARROW_LEFT -> 0x25;
      case ARROW_UP -> 0x26;
      case ARROW_RIGHT -> 0x27;
      case ARROW_DOWN -> 0x28;
      case PRINT_SCREEN -> 0x2C;
      case INSERT -> 0x2D;
      case DELETE -> 0x2E;
      case META -> location == BrowserKeyLocation.RIGHT ? 0x5C : 0x5B;
      case CONTEXT_MENU -> 0x5D;
      case MULTIPLY -> 0x6A;
      case ADD -> 0x6B;
      case SUBTRACT -> 0x6D;
      case DECIMAL -> input.modifiers().contains(BrowserModifier.NUM_LOCK) ? 0x6E : 0x2E;
      case DIVIDE -> 0x6F;
      case NUM_LOCK -> 0x90;
      case SCROLL_LOCK -> 0x91;
      case SEMICOLON -> 0xBA;
      case EQUAL -> 0xBB;
      case COMMA -> 0xBC;
      case MINUS -> 0xBD;
      case PERIOD -> 0xBE;
      case SLASH -> 0xBF;
      case BACKQUOTE -> 0xC0;
      case BRACKET_LEFT -> 0xDB;
      case BACKSLASH -> 0xDC;
      case BRACKET_RIGHT -> 0xDD;
      case QUOTE -> 0xDE;
      case INTERNATIONAL_1, INTERNATIONAL_2 -> 0xE2;
      default -> {
        int functionNumber = functionNumber(key);
        if (functionNumber >= 1 && functionNumber <= 24) {
          yield 0x70 + functionNumber - 1;
        }
        yield layoutCharacter;
      }
    };
  }

  private static int keypadDigitVirtualKey(int digit, boolean numLock) {
    if (numLock) {
      return 0x60 + digit;
    }
    return switch (digit) {
      case 0 -> 0x2D;
      case 1 -> 0x23;
      case 2 -> 0x28;
      case 3 -> 0x22;
      case 4 -> 0x25;
      case 5 -> 0x0C;
      case 6 -> 0x27;
      case 7 -> 0x24;
      case 8 -> 0x26;
      case 9 -> 0x21;
      default -> 0;
    };
  }

  private static int nativeKeyCode(
      BrowserKey key,
      BrowserKeyLocation location,
      BrowserKeyAction action,
      BrowserKeyPlatform platform,
      long scanCode) {
    return switch (platform) {
      case WINDOWS ->
          scanCode == 0
              ? 0
              : CefKeyEvent.buildWindowsNativeKeyCode(
                  scanCode,
                  isExtendedWindowsKey(key, location),
                  action == BrowserKeyAction.RELEASE);
      case MACOS -> macKeyCode(key, location, scanCode);
      case LINUX, OTHER -> (int) Math.min(scanCode, Integer.MAX_VALUE);
    };
  }

  private static long resolveScanCode(
      BrowserKey key,
      BrowserKeyLocation location,
      BrowserKeyPlatform platform,
      long suppliedScanCode) {
    if (platform == BrowserKeyPlatform.WINDOWS) {
      long requiredScanCode = requiredWindowsScanCode(key);
      if (requiredScanCode != 0) {
        return requiredScanCode;
      }
      return suppliedScanCode != 0 ? suppliedScanCode & 0xFFL : windowsScanCode(key, location);
    }
    if (suppliedScanCode != 0) {
      return suppliedScanCode;
    }
    if (platform == BrowserKeyPlatform.MACOS) {
      return Math.max(macKeyCode(key, location, 0), 0);
    }
    return 0;
  }

  private static int requiredWindowsScanCode(BrowserKey key) {
    return switch (key) {
      case CONTROL -> 29;
      case ENTER -> 28;
      case HOME -> 71;
      case ARROW_UP -> 72;
      case PAGE_UP -> 73;
      case ARROW_LEFT -> 75;
      case ARROW_RIGHT -> 77;
      case END -> 79;
      case ARROW_DOWN -> 80;
      case PAGE_DOWN -> 81;
      case DELETE -> 83;
      default -> 0;
    };
  }

  @SuppressWarnings("java:S1479")
  private static int windowsScanCode(BrowserKey key, BrowserKeyLocation location) {
    int digit = digit(key);
    if (digit >= 0) {
      if (location == BrowserKeyLocation.NUMPAD) {
        return switch (digit) {
          case 0 -> 82;
          case 1 -> 79;
          case 2 -> 80;
          case 3 -> 81;
          case 4 -> 75;
          case 5 -> 76;
          case 6 -> 77;
          case 7 -> 71;
          case 8 -> 72;
          case 9 -> 73;
          default -> 0;
        };
      }
      return switch (digit) {
        case 0 -> 11;
        case 1 -> 2;
        case 2 -> 3;
        case 3 -> 4;
        case 4 -> 5;
        case 5 -> 6;
        case 6 -> 7;
        case 7 -> 8;
        case 8 -> 9;
        case 9 -> 10;
        default -> 0;
      };
    }
    int functionNumber = functionNumber(key);
    if (functionNumber >= 1 && functionNumber <= 10) {
      return 58 + functionNumber;
    }
    if (functionNumber >= 11 && functionNumber <= 12) {
      return 76 + functionNumber;
    }
    return switch (key) {
      case ESCAPE -> 1;
      case MINUS -> 12;
      case EQUAL -> location == BrowserKeyLocation.NUMPAD ? 89 : 13;
      case BACKSPACE -> 14;
      case TAB -> 15;
      case KEY_Q -> 16;
      case KEY_W -> 17;
      case KEY_E -> 18;
      case KEY_R -> 19;
      case KEY_T -> 20;
      case KEY_Y -> 21;
      case KEY_U -> 22;
      case KEY_I -> 23;
      case KEY_O -> 24;
      case KEY_P -> 25;
      case BRACKET_LEFT -> 26;
      case BRACKET_RIGHT -> 27;
      case ENTER -> 28;
      case CONTROL -> 29;
      case KEY_A -> 30;
      case KEY_S -> 31;
      case KEY_D -> 32;
      case KEY_F -> 33;
      case KEY_G -> 34;
      case KEY_H -> 35;
      case KEY_J -> 36;
      case KEY_K -> 37;
      case KEY_L -> 38;
      case SEMICOLON -> 39;
      case QUOTE -> 40;
      case BACKQUOTE -> 41;
      case SHIFT -> location == BrowserKeyLocation.RIGHT ? 54 : 42;
      case BACKSLASH -> 43;
      case KEY_Z -> 44;
      case KEY_X -> 45;
      case KEY_C -> 46;
      case KEY_V -> 47;
      case KEY_B -> 48;
      case KEY_N -> 49;
      case KEY_M -> 50;
      case COMMA -> 51;
      case PERIOD -> 52;
      case SLASH, DIVIDE -> 53;
      case MULTIPLY -> 55;
      case ALT -> 56;
      case SPACE -> 57;
      case CAPS_LOCK -> 58;
      case NUM_LOCK -> 69;
      case SCROLL_LOCK -> 70;
      case SUBTRACT -> 74;
      case ADD -> 78;
      case DECIMAL, DELETE -> 83;
      case HOME -> 71;
      case ARROW_UP -> 72;
      case PAGE_UP -> 73;
      case ARROW_LEFT -> 75;
      case ARROW_RIGHT -> 77;
      case END -> 79;
      case ARROW_DOWN -> 80;
      case PAGE_DOWN -> 81;
      case INSERT -> 82;
      case META -> location == BrowserKeyLocation.RIGHT ? 92 : 91;
      case CONTEXT_MENU -> 93;
      default -> 0;
    };
  }

  @SuppressWarnings("java:S1479")
  private static int macKeyCode(BrowserKey key, BrowserKeyLocation location, long fallback) {
    int digit = digit(key);
    if (digit >= 0) {
      if (location == BrowserKeyLocation.NUMPAD) {
        return switch (digit) {
          case 0 -> 82;
          case 1 -> 83;
          case 2 -> 84;
          case 3 -> 85;
          case 4 -> 86;
          case 5 -> 87;
          case 6 -> 88;
          case 7 -> 89;
          case 8 -> 91;
          case 9 -> 92;
          default -> (int) fallback;
        };
      }
      return switch (digit) {
        case 0 -> 29;
        case 1 -> 18;
        case 2 -> 19;
        case 3 -> 20;
        case 4 -> 21;
        case 5 -> 23;
        case 6 -> 22;
        case 7 -> 26;
        case 8 -> 28;
        case 9 -> 25;
        default -> (int) fallback;
      };
    }
    return switch (key) {
      case KEY_A -> 0;
      case KEY_S -> 1;
      case KEY_D -> 2;
      case KEY_F -> 3;
      case KEY_H -> 4;
      case KEY_G -> 5;
      case KEY_Z -> 6;
      case KEY_X -> 7;
      case KEY_C -> 8;
      case KEY_V -> 9;
      case KEY_B -> 11;
      case KEY_Q -> 12;
      case KEY_W -> 13;
      case KEY_E -> 14;
      case KEY_R -> 15;
      case KEY_Y -> 16;
      case KEY_T -> 17;
      case EQUAL -> location == BrowserKeyLocation.NUMPAD ? 81 : 24;
      case MINUS -> 27;
      case BRACKET_RIGHT -> 30;
      case KEY_O -> 31;
      case KEY_U -> 32;
      case BRACKET_LEFT -> 33;
      case KEY_I -> 34;
      case KEY_P -> 35;
      case ENTER -> location == BrowserKeyLocation.NUMPAD ? 76 : 36;
      case KEY_L -> 37;
      case KEY_J -> 38;
      case QUOTE -> 39;
      case KEY_K -> 40;
      case SEMICOLON -> 41;
      case BACKSLASH -> 42;
      case COMMA -> 43;
      case SLASH -> 44;
      case KEY_N -> 45;
      case KEY_M -> 46;
      case PERIOD -> 47;
      case TAB -> 48;
      case SPACE -> 49;
      case BACKQUOTE -> 50;
      case BACKSPACE -> 51;
      case ESCAPE -> 53;
      case META -> location == BrowserKeyLocation.RIGHT ? 54 : 55;
      case SHIFT -> location == BrowserKeyLocation.RIGHT ? 60 : 56;
      case CAPS_LOCK -> 57;
      case ALT -> location == BrowserKeyLocation.RIGHT ? 61 : 58;
      case CONTROL -> location == BrowserKeyLocation.RIGHT ? 62 : 59;
      case DECIMAL -> 65;
      case MULTIPLY -> 67;
      case ADD -> 69;
      case NUM_LOCK -> 71;
      case DIVIDE -> 75;
      case SUBTRACT -> 78;
      case F17 -> 64;
      case F18 -> 79;
      case F19 -> 80;
      case F20 -> 90;
      case F5 -> 96;
      case F6 -> 97;
      case F7 -> 98;
      case F3 -> 99;
      case F8 -> 100;
      case F9 -> 101;
      case F11 -> 103;
      case F13 -> 105;
      case F16 -> 106;
      case F14 -> 107;
      case F10 -> 109;
      case F12 -> 111;
      case F15 -> 113;
      case HOME -> 115;
      case PAGE_UP -> 116;
      case DELETE -> 117;
      case F4 -> 118;
      case END -> 119;
      case F2 -> 120;
      case PAGE_DOWN -> 121;
      case F1 -> 122;
      case ARROW_LEFT -> 123;
      case ARROW_RIGHT -> 124;
      case ARROW_DOWN -> 125;
      case ARROW_UP -> 126;
      default -> (int) Math.min(fallback, Integer.MAX_VALUE);
    };
  }

  private static char unmodifiedCharacter(
      BrowserKey key, BrowserKeyPlatform platform, char layoutCharacter) {
    return switch (platform) {
      case WINDOWS -> 0;
      case MACOS ->
          switch (key) {
            case BACKSPACE -> 0x7F;
            case ARROW_LEFT -> '\uF702';
            case ARROW_RIGHT -> '\uF703';
            case ARROW_UP -> '\uF700';
            case ARROW_DOWN -> '\uF701';
            case INSERT -> '\uF727';
            case DELETE -> '\uF728';
            case HOME -> '\uF729';
            case END -> '\uF72B';
            case PAGE_UP -> '\uF72C';
            case PAGE_DOWN -> '\uF72D';
            default -> layoutCharacter;
          };
      case LINUX ->
          switch (key) {
            case BACKSPACE -> (char) 0xFF08;
            case TAB -> (char) 0xFF09;
            case ENTER -> '\r';
            case ESCAPE -> (char) 0xFF1B;
            case ARROW_LEFT -> (char) 0xFF51;
            case ARROW_UP -> (char) 0xFF52;
            case ARROW_RIGHT -> (char) 0xFF53;
            case ARROW_DOWN -> (char) 0xFF54;
            case DELETE -> (char) 0xFFFF;
            default -> layoutCharacter;
          };
      case OTHER -> layoutCharacter;
    };
  }

  private static char character(
      BrowserKey key, char unmodifiedCharacter, boolean control, boolean shift) {
    if (!control) {
      return unmodifiedCharacter;
    }
    int letter = letter(key);
    if (letter >= 0) {
      return (char) (letter + 1);
    }
    return switch (key) {
      case BRACKET_LEFT -> 0x1B;
      case BACKSLASH -> 0x1C;
      case BRACKET_RIGHT -> 0x1D;
      case DIGIT_6 -> shift ? 0x1E : unmodifiedCharacter;
      case MINUS -> shift ? 0x1F : unmodifiedCharacter;
      case ENTER -> '\n';
      default -> unmodifiedCharacter;
    };
  }

  private static boolean isExtendedWindowsKey(BrowserKey key, BrowserKeyLocation location) {
    if ((key == BrowserKey.CONTROL || key == BrowserKey.ALT)
        && location == BrowserKeyLocation.RIGHT) {
      return true;
    }
    if (key == BrowserKey.ENTER && location == BrowserKeyLocation.NUMPAD) {
      return true;
    }
    return switch (key) {
      case INSERT,
          DELETE,
          HOME,
          END,
          PAGE_UP,
          PAGE_DOWN,
          ARROW_UP,
          ARROW_DOWN,
          ARROW_LEFT,
          ARROW_RIGHT,
          DIVIDE,
          NUM_LOCK,
          PRINT_SCREEN,
          META,
          CONTEXT_MENU ->
          true;
      default -> false;
    };
  }

  private static int letter(BrowserKey key) {
    return switch (key) {
      case KEY_A -> 0;
      case KEY_B -> 1;
      case KEY_C -> 2;
      case KEY_D -> 3;
      case KEY_E -> 4;
      case KEY_F -> 5;
      case KEY_G -> 6;
      case KEY_H -> 7;
      case KEY_I -> 8;
      case KEY_J -> 9;
      case KEY_K -> 10;
      case KEY_L -> 11;
      case KEY_M -> 12;
      case KEY_N -> 13;
      case KEY_O -> 14;
      case KEY_P -> 15;
      case KEY_Q -> 16;
      case KEY_R -> 17;
      case KEY_S -> 18;
      case KEY_T -> 19;
      case KEY_U -> 20;
      case KEY_V -> 21;
      case KEY_W -> 22;
      case KEY_X -> 23;
      case KEY_Y -> 24;
      case KEY_Z -> 25;
      default -> -1;
    };
  }

  private static int digit(BrowserKey key) {
    return switch (key) {
      case DIGIT_0 -> 0;
      case DIGIT_1 -> 1;
      case DIGIT_2 -> 2;
      case DIGIT_3 -> 3;
      case DIGIT_4 -> 4;
      case DIGIT_5 -> 5;
      case DIGIT_6 -> 6;
      case DIGIT_7 -> 7;
      case DIGIT_8 -> 8;
      case DIGIT_9 -> 9;
      default -> -1;
    };
  }

  private static int functionNumber(BrowserKey key) {
    return switch (key) {
      case F1 -> 1;
      case F2 -> 2;
      case F3 -> 3;
      case F4 -> 4;
      case F5 -> 5;
      case F6 -> 6;
      case F7 -> 7;
      case F8 -> 8;
      case F9 -> 9;
      case F10 -> 10;
      case F11 -> 11;
      case F12 -> 12;
      case F13 -> 13;
      case F14 -> 14;
      case F15 -> 15;
      case F16 -> 16;
      case F17 -> 17;
      case F18 -> 18;
      case F19 -> 19;
      case F20 -> 20;
      case F21 -> 21;
      case F22 -> 22;
      case F23 -> 23;
      case F24 -> 24;
      case F25 -> 25;
      default -> 0;
    };
  }

  record ResolvedKey(
      BrowserKeyPlatform platform,
      int windowsKeyCode,
      int nativeKeyCode,
      boolean systemKey,
      char character,
      char unmodifiedCharacter,
      long scanCode) {}
}
