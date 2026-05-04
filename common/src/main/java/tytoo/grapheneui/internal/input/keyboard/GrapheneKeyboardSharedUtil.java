package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.keyboard.mapping.GrapheneKeyboardMappings;

import java.awt.event.KeyEvent;

public final class GrapheneKeyboardSharedUtil {
    private static final char MAC_FUNCTION_KEY_START = '\uF700';
    private static final char MAC_FUNCTION_KEY_END = '\uF8FF';

    private GrapheneKeyboardSharedUtil() {
    }

    static char resolveLayoutCharacter(int keyCode, int scanCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        char fallbackCharacter = GrapheneKeyboardMappings.charFromKeyCode(keyCode, shift);
        if (GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            return fallbackCharacter;
        }

        if (!shouldResolveLayoutKeyName(keyCode, fallbackCharacter)) {
            return fallbackCharacter;
        }

        String keyName;
        try {
            keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        } catch (UnsatisfiedLinkError exception) {
            return fallbackCharacter;
        }
        if (keyName == null || keyName.isBlank()) {
            return fallbackCharacter;
        }

        int codePoint = keyName.codePointAt(0);
        if (!Character.isValidCodePoint(codePoint) || Character.isSupplementaryCodePoint(codePoint)) {
            return fallbackCharacter;
        }

        char layoutCharacter = (char) codePoint;
        if (shift) {
            char shiftedLayoutCharacter = resolveShiftedLayoutCharacter(keyCode, layoutCharacter);
            if (shiftedLayoutCharacter != KeyEvent.CHAR_UNDEFINED) {
                return shiftedLayoutCharacter;
            }
        }

        if (shift && fallbackCharacter != KeyEvent.CHAR_UNDEFINED) {
            char unshiftedFallbackCharacter = GrapheneKeyboardMappings.charFromKeyCode(keyCode, false);
            if (layoutCharacter == unshiftedFallbackCharacter) {
                return fallbackCharacter;
            }
        }

        if (shift && Character.isLetter(layoutCharacter)) {
            return Character.toUpperCase(layoutCharacter);
        }

        return layoutCharacter;
    }

    static int resolveWindowsVirtualKeyCode(int keyCode, char character, boolean numLockEnabled) {
        int numpadKeyCode = resolveNumpadWindowsVirtualKeyCode(keyCode, numLockEnabled);
        if (numpadKeyCode != 0) {
            return numpadKeyCode;
        }

        if (isAsciiLetter(character)) {
            return Character.toUpperCase(character);
        }

        int layoutMappedKeyCode = GrapheneKeyboardMappings.windowsVkFromLayoutPair(keyCode, character);
        if (layoutMappedKeyCode != 0) {
            return layoutMappedKeyCode;
        }

        if (character != KeyEvent.CHAR_UNDEFINED && !isAsciiPrintable(character)) {
            int charMappedKeyCode = GrapheneKeyboardMappings.windowsVkFromCharacter(character);
            if (charMappedKeyCode != 0) {
                return charMappedKeyCode;
            }
        }

        int mappedKeyCode = GrapheneKeyboardMappings.windowsVkFromGlfw(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        if (character == KeyEvent.CHAR_UNDEFINED) {
            return 0;
        }

        int charMappedKeyCode = GrapheneKeyboardMappings.windowsVkFromCharacter(character);
        return charMappedKeyCode != 0 ? charMappedKeyCode : character;
    }

    static String normalizeTypedText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        if (text.codePointCount(0, text.length()) != 1) {
            return text;
        }

        int codePoint = text.codePointAt(0);
        if (!Character.isSupplementaryCodePoint(codePoint)) {
            char normalizedCharacter = normalizeTypedCharacter((char) codePoint);
            if (normalizedCharacter == KeyEvent.CHAR_UNDEFINED) {
                return "";
            }

            return String.valueOf(normalizedCharacter);
        }

        return isUnsupportedTypedCodePoint(codePoint) ? "" : text;
    }

    public static char normalizeTypedCharacter(char character) {
        if (character == 0x7F) {
            return '\b';
        }

        if (character == '\n') {
            return '\r';
        }

        if (isUnsupportedTypedCharacter(character)) {
            return KeyEvent.CHAR_UNDEFINED;
        }

        return character;
    }

    private static int resolveNumpadWindowsVirtualKeyCode(int keyCode, boolean numLockEnabled) {
        if (!GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            return 0;
        }

        if (!numLockEnabled) {
            int mappedLogicalKeyCode = switch (keyCode) {
                case GLFW.GLFW_KEY_KP_0 -> KeyEvent.VK_INSERT;
                case GLFW.GLFW_KEY_KP_1 -> KeyEvent.VK_END;
                case GLFW.GLFW_KEY_KP_2 -> KeyEvent.VK_DOWN;
                case GLFW.GLFW_KEY_KP_3 -> KeyEvent.VK_PAGE_DOWN;
                case GLFW.GLFW_KEY_KP_4 -> KeyEvent.VK_LEFT;
                case GLFW.GLFW_KEY_KP_5 -> KeyEvent.VK_CLEAR;
                case GLFW.GLFW_KEY_KP_6 -> KeyEvent.VK_RIGHT;
                case GLFW.GLFW_KEY_KP_7 -> KeyEvent.VK_HOME;
                case GLFW.GLFW_KEY_KP_8 -> KeyEvent.VK_UP;
                case GLFW.GLFW_KEY_KP_9 -> KeyEvent.VK_PAGE_UP;
                case GLFW.GLFW_KEY_KP_DECIMAL -> KeyEvent.VK_DELETE;
                default -> 0;
            };
            if (mappedLogicalKeyCode != 0) {
                return mappedLogicalKeyCode;
            }
        }

        return GrapheneKeyboardMappings.windowsVkFromGlfw(keyCode);
    }

    private static boolean isAsciiLetter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    private static boolean isAsciiPrintable(char character) {
        return character >= 0x20 && character <= 0x7E;
    }

    static char resolveShiftedLayoutCharacter(int keyCode, char layoutCharacter) {
        return resolveAzertyShiftedDigit(keyCode, layoutCharacter);
    }

    private static char resolveAzertyShiftedDigit(int keyCode, char layoutCharacter) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_0 -> layoutCharacter == 0x00E0 ? '0' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_1 -> layoutCharacter == '&' ? '1' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_2 -> layoutCharacter == 0x00E9 ? '2' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_3 -> layoutCharacter == '"' ? '3' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_4 -> layoutCharacter == '\'' ? '4' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_5 -> layoutCharacter == '(' ? '5' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_6 -> layoutCharacter == '-' ? '6' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_7 -> layoutCharacter == 0x00E8 ? '7' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_8 -> layoutCharacter == '_' ? '8' : KeyEvent.CHAR_UNDEFINED;
            case GLFW.GLFW_KEY_9 -> layoutCharacter == 0x00E7 ? '9' : KeyEvent.CHAR_UNDEFINED;
            default -> KeyEvent.CHAR_UNDEFINED;
        };
    }

    private static boolean shouldResolveLayoutKeyName(int keyCode, char fallbackCharacter) {
        if (keyCode == GLFW.GLFW_KEY_WORLD_1 || keyCode == GLFW.GLFW_KEY_WORLD_2) {
            return true;
        }

        return fallbackCharacter != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(fallbackCharacter);
    }

    private static boolean isUnsupportedTypedCharacter(char character) {
        return isUnsupportedTypedCodePoint(character);
    }

    private static boolean isUnsupportedTypedCodePoint(int codePoint) {
        if (codePoint >= MAC_FUNCTION_KEY_START && codePoint <= MAC_FUNCTION_KEY_END) {
            return true;
        }

        return Character.isISOControl(codePoint)
                && codePoint != '\b'
                && codePoint != '\t'
                && codePoint != '\r';
    }
}
