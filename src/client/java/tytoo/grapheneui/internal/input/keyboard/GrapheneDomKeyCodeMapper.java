package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

final class GrapheneDomKeyCodeMapper {
    private GrapheneDomKeyCodeMapper() {
    }

    static int resolveDomKeyCode(int keyCode, char character, boolean numLockEnabled) {
        if (GrapheneKeyCodeUtil.isNumpadKey(keyCode)) {
            if (GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) && !numLockEnabled) {
                return 0;
            }
            int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
            if (mappedKeyCode != 0) {
                return mappedKeyCode;
            }
        }

        if (isAsciiLetter(character)) {
            return Character.toUpperCase(character);
        }

        int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        int charMappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCodeFromCharacter(character);
        return charMappedKeyCode != 0 ? charMappedKeyCode : character;
    }

    static boolean isAsciiLetter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    @SuppressWarnings("java:S1479")
    static boolean isLayoutDependentKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_A,
                 GLFW.GLFW_KEY_B,
                 GLFW.GLFW_KEY_C,
                 GLFW.GLFW_KEY_D,
                 GLFW.GLFW_KEY_E,
                 GLFW.GLFW_KEY_F,
                 GLFW.GLFW_KEY_G,
                 GLFW.GLFW_KEY_H,
                 GLFW.GLFW_KEY_I,
                 GLFW.GLFW_KEY_J,
                 GLFW.GLFW_KEY_K,
                 GLFW.GLFW_KEY_L,
                 GLFW.GLFW_KEY_M,
                 GLFW.GLFW_KEY_N,
                 GLFW.GLFW_KEY_O,
                 GLFW.GLFW_KEY_P,
                 GLFW.GLFW_KEY_Q,
                 GLFW.GLFW_KEY_R,
                 GLFW.GLFW_KEY_S,
                 GLFW.GLFW_KEY_T,
                 GLFW.GLFW_KEY_U,
                 GLFW.GLFW_KEY_V,
                 GLFW.GLFW_KEY_W,
                 GLFW.GLFW_KEY_X,
                 GLFW.GLFW_KEY_Y,
                 GLFW.GLFW_KEY_Z,
                 GLFW.GLFW_KEY_0,
                 GLFW.GLFW_KEY_1,
                 GLFW.GLFW_KEY_2,
                 GLFW.GLFW_KEY_3,
                 GLFW.GLFW_KEY_4,
                 GLFW.GLFW_KEY_5,
                 GLFW.GLFW_KEY_6,
                 GLFW.GLFW_KEY_7,
                 GLFW.GLFW_KEY_8,
                 GLFW.GLFW_KEY_9,
                 GLFW.GLFW_KEY_GRAVE_ACCENT,
                 GLFW.GLFW_KEY_MINUS,
                 GLFW.GLFW_KEY_EQUAL,
                 GLFW.GLFW_KEY_LEFT_BRACKET,
                 GLFW.GLFW_KEY_RIGHT_BRACKET,
                 GLFW.GLFW_KEY_BACKSLASH,
                 GLFW.GLFW_KEY_SEMICOLON,
                 GLFW.GLFW_KEY_APOSTROPHE,
                 GLFW.GLFW_KEY_COMMA,
                 GLFW.GLFW_KEY_PERIOD,
                 GLFW.GLFW_KEY_SLASH,
                 GLFW.GLFW_KEY_WORLD_1,
                 GLFW.GLFW_KEY_WORLD_2 -> true;
            default -> false;
        };
    }
}
