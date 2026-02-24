package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

import java.awt.event.KeyEvent;

abstract class GrapheneBaseKeyEventPlatformResolver implements GrapheneKeyEventPlatformResolver {
    @Override
    public int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
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
    public int getCharNativeKeyCode(char character) {
        return character;
    }

    @Override
    public boolean isSystemKey(int modifiers) {
        return false;
    }

    @Override
    public int getRawEventType(boolean pressed, int keyCode, char character) {
        return pressed ? CefKeyEvent.KEYEVENT_RAWKEYDOWN : CefKeyEvent.KEYEVENT_KEYUP;
    }

    @Override
    public int resolveScanCode(int keyCode, int scanCode) {
        return scanCode;
    }

    @Override
    public long getScanCode(int scanCode) {
        return scanCode <= 0 ? 0L : scanCode;
    }

    @Override
    public char getRawEventUnmodifiedCharacter(int keyCode, char character, int modifiers) {
        return toRawEventCharacter(character);
    }

    @Override
    public char getRawEventCharacter(int keyCode, char unmodifiedCharacter, int modifiers) {
        return unmodifiedCharacter;
    }

    @Override
    public int sanitizeCharEventModifiers(int modifiers, boolean rightAltPressed) {
        return modifiers;
    }

    @Override
    public char toRawEventCharacter(char character) {
        return KeyEvent.CHAR_UNDEFINED;
    }

    @Override
    public char resolveRawKeyCharacter(int keyCode, char layoutCharacter) {
        return layoutCharacter;
    }
}
