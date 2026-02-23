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

    int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed);

    int getCharNativeKeyCode(char character);

    boolean isSystemKey(int modifiers);

    int getRawEventType(boolean pressed, int keyCode, char character);

    long getScanCode(int scanCode);

    int sanitizeCharEventModifiers(int modifiers, boolean rightAltPressed);

    char toRawEventCharacter(char character);

    char resolveRawKeyCharacter(int keyCode, char layoutCharacter);
}
