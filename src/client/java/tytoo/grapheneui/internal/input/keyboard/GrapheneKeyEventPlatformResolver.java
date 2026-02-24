package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.awt.event.KeyEvent;

interface GrapheneKeyEventPlatformResolver {
    static GrapheneKeyEventPlatformResolver create() {
        if (GraphenePlatform.isLinux()) {
            return new GrapheneLinuxKeyEventPlatformResolver();
        }

        if (GraphenePlatform.isMac()) {
            return new GrapheneMacKeyEventPlatformResolver();
        }

        if (GraphenePlatform.isWindows()) {
            return new GrapheneWindowsKeyEventPlatformResolver();
        }

        return new GrapheneLinuxKeyEventPlatformResolver();
    }

    default int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (scanCode > 0) {
            return scanCode;
        }

        int mappedKeyCode = GrapheneKeyboardMappings.windowsVkFromGlfw(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        return character;
    }

    default int getCharNativeKeyCode(char character) {
        return character;
    }

    default boolean isSystemKey(int modifiers) {
        return false;
    }

    default int getRawEventType(boolean pressed, int keyCode, char character) {
        return pressed ? CefKeyEvent.KEYEVENT_RAWKEYDOWN : CefKeyEvent.KEYEVENT_KEYUP;
    }

    default int resolveScanCode(int keyCode, int scanCode) {
        return scanCode;
    }

    default long getScanCode(int scanCode) {
        return scanCode <= 0 ? 0L : scanCode;
    }

    default char getRawEventUnmodifiedCharacter(int keyCode, char character, int modifiers) {
        return KeyEvent.CHAR_UNDEFINED;
    }

    default char getRawEventCharacter(int keyCode, char unmodifiedCharacter, int modifiers) {
        return unmodifiedCharacter;
    }

    default int sanitizeCharEventModifiers(int modifiers, boolean rightAltPressed) {
        return modifiers;
    }

    default char resolveRawKeyCharacter(int keyCode, char layoutCharacter) {
        return layoutCharacter;
    }
}
