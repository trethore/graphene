package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

import java.awt.event.KeyEvent;

final class GrapheneCharacterMapper {
    private static final char MAC_FUNCTION_KEY_START = '\uF700';
    private static final char MAC_FUNCTION_KEY_END = '\uF8FF';

    private GrapheneCharacterMapper() {
    }

    static char resolveLayoutCharacter(int keyCode, int scanCode, int modifiers) {
        if (GrapheneKeyCodeUtil.isNumpadKey(keyCode)) {
            return GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        }

        String keyName = null;
        if (scanCode > 0) {
            keyName = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            return GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        }

        int codePoint = keyName.codePointAt(0);
        if (!Character.isValidCodePoint(codePoint) || Character.isSupplementaryCodePoint(codePoint)) {
            return GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        }

        char layoutCharacter = (char) codePoint;
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && Character.isLetter(layoutCharacter)) {
            return Character.toUpperCase(layoutCharacter);
        }

        return layoutCharacter;
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
