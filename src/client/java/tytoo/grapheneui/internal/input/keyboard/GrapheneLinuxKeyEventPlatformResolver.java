package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

import java.awt.event.KeyEvent;

final class GrapheneLinuxKeyEventPlatformResolver extends GrapheneBaseKeyEventPlatformResolver {
    private static boolean isPrintableCharacter(char character) {
        return character >= 0x20 && !Character.isISOControl(character);
    }

    private static char resolveLinuxLayoutCharacter(int keyCode, int scanCode) {
        String keyName = null;
        if (scanCode > 0) {
            keyName = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        }

        if (keyName == null || keyName.isBlank()) {
            return KeyEvent.CHAR_UNDEFINED;
        }

        int codePoint = keyName.codePointAt(0);
        if (!Character.isValidCodePoint(codePoint) || Character.isSupplementaryCodePoint(codePoint)) {
            return KeyEvent.CHAR_UNDEFINED;
        }

        return (char) codePoint;
    }

    @Override
    public char resolveRawKeyCharacter(int keyCode, int scanCode, int modifiers) {
        if (GrapheneKeyCodeUtil.isNumpadKey(keyCode)) {
            return GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        }

        char layoutCharacter = resolveLinuxLayoutCharacter(keyCode, scanCode);
        if (layoutCharacter == KeyEvent.CHAR_UNDEFINED) {
            return GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        }

        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && Character.isLetter(layoutCharacter)) {
            return Character.toUpperCase(layoutCharacter);
        }

        return layoutCharacter;
    }

    @Override
    public char toRawEventCharacter(char character) {
        return character;
    }

    @Override
    public int resolveWindowsKeyCode(int keyCode, int scanCode, char character, boolean numLockEnabled) {
        int linuxMappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCodeFromLinuxCharacter(character, keyCode);
        if (linuxMappedKeyCode != 0) {
            return linuxMappedKeyCode;
        }

        if (isAlphabetKey(keyCode) && Character.isLetter(character)) {
            return Character.toUpperCase(character);
        }

        int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
        return mappedKeyCode != 0 ? mappedKeyCode : character;
    }

    @Override
    public int resolveNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (isPrintableCharacter(character)) {
            return character;
        }

        return super.resolveNativeKeyCode(keyCode, scanCode, character, pressed);
    }

    @Override
    public int resolveRawKeyEventType(boolean pressed, int keyCode, char character) {
        if (!pressed) {
            return CefKeyEvent.KEYEVENT_KEYUP;
        }

        if (isLayoutDependentKey(keyCode) && isPrintableCharacter(character)) {
            return CefKeyEvent.KEYEVENT_KEYDOWN;
        }

        return CefKeyEvent.KEYEVENT_RAWKEYDOWN;
    }

    @Override
    public boolean isSystemKey(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }
}
