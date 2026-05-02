package tytoo.grapheneui.internal.input.keyboard.mapping;

public final class GrapheneKeyboardMappings {
    private GrapheneKeyboardMappings() {
    }

    public static int windowsVkFromGlfw(int keyCode) {
        return GrapheneWindowsVirtualKeyMappings.fromGlfw(keyCode);
    }

    public static int windowsVkFromCharacter(char character) {
        return GrapheneWindowsVirtualKeyMappings.fromCharacter(character);
    }

    public static int windowsVkFromLayoutPair(int keyCode, char character) {
        return GrapheneWindowsVirtualKeyMappings.fromLayoutPair(keyCode, character);
    }

    public static int macNativeFromGlfw(int keyCode) {
        return GrapheneMacNativeKeyMappings.fromGlfw(keyCode);
    }

    public static boolean hasMacNativeFromGlfw(int keyCode) {
        return GrapheneMacNativeKeyMappings.hasGlfwMapping(keyCode);
    }

    public static int macNativeFromCharacter(char character) {
        return GrapheneMacNativeKeyMappings.fromCharacter(character);
    }

    public static boolean hasMacNativeFromCharacter(char character) {
        return GrapheneMacNativeKeyMappings.hasCharacterMapping(character);
    }

    public static boolean isNumpadKey(int keyCode) {
        return GrapheneNumpadMappings.isNumpadKey(keyCode);
    }

    public static boolean isNumpadTextKey(int keyCode) {
        return GrapheneNumpadMappings.isTextKey(keyCode);
    }

    public static boolean requiresNumLockForText(int keyCode) {
        return GrapheneNumpadMappings.requiresNumLockForText(keyCode);
    }

    public static String domCodeFromGlfw(int keyCode) {
        return GrapheneDomCodeMappings.fromGlfw(keyCode);
    }

    public static String domKeyFromGlfw(int keyCode) {
        return GrapheneDomKeyMappings.fromGlfw(keyCode);
    }

    public static String domKeyFromNumpadKey(int keyCode, boolean numLockEnabled) {
        return GrapheneNumpadMappings.domKeyFromGlfw(keyCode, numLockEnabled);
    }

    public static boolean isLeftModifierKey(int keyCode) {
        return GrapheneModifierKeyMappings.isLeftModifierKey(keyCode);
    }

    public static boolean isRightModifierKey(int keyCode) {
        return GrapheneModifierKeyMappings.isRightModifierKey(keyCode);
    }

    public static char charFromKeyCode(int keyCode, boolean shift) {
        return GrapheneFallbackCharacterMappings.fromKeyCode(keyCode, shift);
    }
}
