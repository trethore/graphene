package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

final class GrapheneKeyboardSharedUtil {
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

        String keyName = null;
        if (scanCode > 0) {
            keyName = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            return fallbackCharacter;
        }

        int codePoint = keyName.codePointAt(0);
        if (!Character.isValidCodePoint(codePoint) || Character.isSupplementaryCodePoint(codePoint)) {
            return fallbackCharacter;
        }

        char layoutCharacter = (char) codePoint;
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
        if (GrapheneKeyboardMappings.isNumpadKey(keyCode) && !numLockEnabled) {
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

        if (GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            int mappedKeyCode = GrapheneKeyboardMappings.windowsVkFromGlfw(keyCode);
            if (mappedKeyCode != 0) {
                return mappedKeyCode;
            }
        }

        if (isAsciiLetter(character)) {
            return Character.toUpperCase(character);
        }

        int layoutMappedKeyCode = GrapheneKeyboardMappings.windowsVkFromLayoutPair(keyCode, character);
        if (layoutMappedKeyCode != 0) {
            return layoutMappedKeyCode;
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

    static char normalizeTypedCharacter(char character) {
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

    private static boolean isAsciiLetter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
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
