package tytoo.grapheneui.internal.input.keyboard;

import tytoo.grapheneui.internal.platform.GraphenePlatform;

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

        return new GrapheneDefaultKeyEventPlatformResolver();
    }

    char resolveRawKeyCharacter(int keyCode, int scanCode, int modifiers);

    char toRawEventCharacter(char character);

    int resolveWindowsKeyCode(int keyCode, int scanCode, char character, boolean numLockEnabled);

    int resolveNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed);

    int resolveRawKeyEventType(boolean pressed, int keyCode, char character);

    long resolveScanCode(int scanCode);

    int resolveCharNativeKeyCode(char character);

    boolean isSystemKey(int modifiers);

    int sanitizeCharEventModifiers(int modifiers, boolean rightAltPressed);

    char normalizeTypedCharacter(char character);
}
