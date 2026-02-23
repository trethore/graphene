package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

import java.awt.event.KeyEvent;

abstract class GrapheneBaseKeyEventPlatformResolver implements GrapheneKeyEventPlatformResolver {
    protected static boolean isAlphabetKey(int keyCode) {
        return keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z;
    }

    @SuppressWarnings("java:S1479")
    protected static boolean isLayoutDependentKey(int keyCode) {
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

    @Override
    public char resolveRawKeyCharacter(int keyCode, int scanCode, int modifiers) {
        return GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
    }

    @Override
    public char toRawEventCharacter(char character) {
        return KeyEvent.CHAR_UNDEFINED;
    }

    @Override
    public int resolveWindowsKeyCode(int keyCode, int scanCode, char character, boolean numLockEnabled) {
        int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        if (isAlphabetKey(keyCode) && Character.isLetter(character)) {
            return Character.toUpperCase(character);
        }

        return character;
    }

    @Override
    public int resolveNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (scanCode > 0) {
            return scanCode;
        }

        int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        return character;
    }

    @Override
    public int resolveRawKeyEventType(boolean pressed, int keyCode, char character) {
        return pressed ? CefKeyEvent.KEYEVENT_RAWKEYDOWN : CefKeyEvent.KEYEVENT_KEYUP;
    }

    @Override
    public long resolveScanCode(int scanCode) {
        return scanCode <= 0 ? 0L : scanCode;
    }

    @Override
    public int resolveCharNativeKeyCode(char character) {
        return character;
    }

    @Override
    public boolean isSystemKey(int modifiers) {
        return false;
    }

    @Override
    public int sanitizeCharEventModifiers(int modifiers, boolean rightAltPressed) {
        return modifiers;
    }

    @Override
    public char normalizeTypedCharacter(char character) {
        return character;
    }
}
