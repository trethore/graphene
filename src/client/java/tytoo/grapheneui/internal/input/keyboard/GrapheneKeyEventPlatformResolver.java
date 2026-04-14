package tytoo.grapheneui.internal.input.keyboard;

import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.awt.event.KeyEvent;

interface GrapheneKeyEventPlatformResolver {
    char KEY_CHARACTER_UNDEFINED = KeyEvent.CHAR_UNDEFINED;

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

    default int getNativeVirtualKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (scanCode > 0) {
            return scanCode;
        }

        int mappedKeyCode = GrapheneKeyboardMappings.windowsVkFromGlfw(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        if (character == KEY_CHARACTER_UNDEFINED) {
            return 0;
        }

        return character;
    }

    default boolean isSystemKey(int modifiers) {
        return false;
    }

    default int resolveScanCode(int keyCode, int scanCode) {
        return scanCode;
    }

    default int sanitizeTextModifiers(int modifiers, boolean rightAltPressed) {
        return modifiers;
    }
}
