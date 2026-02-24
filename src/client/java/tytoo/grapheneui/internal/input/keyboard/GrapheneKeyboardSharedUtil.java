package tytoo.grapheneui.internal.input.keyboard;

import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneInputModifierUtil;

import java.awt.event.KeyEvent;

final class GrapheneKeyboardSharedUtil {
    private static final char MAC_FUNCTION_KEY_START = '\uF700';
    private static final char MAC_FUNCTION_KEY_END = '\uF8FF';

    private GrapheneKeyboardSharedUtil() {
    }

    static int toCefKeyboardModifiers(int modifiers, int keyCode, boolean numLockEnabled) {
        int cefModifiers = GrapheneInputModifierUtil.toCefCommonModifiers(modifiers);

        if ((modifiers & GLFW.GLFW_MOD_CAPS_LOCK) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_CAPS_LOCK_ON;
        }

        if (numLockEnabled) {
            cefModifiers |= EventFlags.EVENTFLAG_NUM_LOCK_ON;
        }

        if (GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            cefModifiers |= EventFlags.EVENTFLAG_IS_KEY_PAD;
        }

        if (isLeftModifierKey(keyCode)) {
            cefModifiers |= EventFlags.EVENTFLAG_IS_LEFT;
        } else if (isRightModifierKey(keyCode)) {
            cefModifiers |= EventFlags.EVENTFLAG_IS_RIGHT;
        }

        return cefModifiers;
    }

    static char resolveLayoutCharacter(int keyCode, int scanCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            return GrapheneKeyboardMappings.charFromKeyCode(keyCode, shift);
        }

        String keyName = null;
        if (scanCode > 0) {
            keyName = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            return GrapheneKeyboardMappings.charFromKeyCode(keyCode, shift);
        }

        int codePoint = keyName.codePointAt(0);
        if (!Character.isValidCodePoint(codePoint) || Character.isSupplementaryCodePoint(codePoint)) {
            return GrapheneKeyboardMappings.charFromKeyCode(keyCode, shift);
        }

        char layoutCharacter = (char) codePoint;
        if (shift && Character.isLetter(layoutCharacter)) {
            return Character.toUpperCase(layoutCharacter);
        }

        return layoutCharacter;
    }

    static int resolveDomKeyCode(int keyCode, char character, boolean numLockEnabled) {
        if (GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            if (GrapheneKeyboardMappings.requiresNumLockForText(keyCode) && !numLockEnabled) {
                return 0;
            }

            int mappedKeyCode = GrapheneKeyboardMappings.windowsVkFromGlfw(keyCode);
            if (mappedKeyCode != 0) {
                return mappedKeyCode;
            }
        }

        if (isAsciiLetter(character)) {
            return Character.toUpperCase(character);
        }

        int mappedKeyCode = GrapheneKeyboardMappings.windowsVkFromGlfw(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        int charMappedKeyCode = GrapheneKeyboardMappings.windowsVkFromCharacter(character);
        return charMappedKeyCode != 0 ? charMappedKeyCode : character;
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

    private static boolean isLeftModifierKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT,
                 GLFW.GLFW_KEY_LEFT_CONTROL,
                 GLFW.GLFW_KEY_LEFT_ALT,
                 GLFW.GLFW_KEY_LEFT_SUPER -> true;
            default -> false;
        };
    }

    private static boolean isRightModifierKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT,
                 GLFW.GLFW_KEY_RIGHT_CONTROL,
                 GLFW.GLFW_KEY_RIGHT_ALT,
                 GLFW.GLFW_KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }

    private static boolean isAsciiLetter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    private static boolean isUnsupportedTypedCharacter(char character) {
        if (character >= MAC_FUNCTION_KEY_START && character <= MAC_FUNCTION_KEY_END) {
            return true;
        }

        return Character.isISOControl(character)
                && character != '\b'
                && character != '\t'
                && character != '\r';
    }
}
