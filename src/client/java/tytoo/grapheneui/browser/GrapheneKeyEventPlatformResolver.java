package tytoo.grapheneui.browser;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.input.GrapheneKeyCodeUtil;
import tytoo.grapheneui.platform.GraphenePlatform;

import java.awt.event.KeyEvent;

interface GrapheneKeyEventPlatformResolver {
    static GrapheneKeyEventPlatformResolver create() {
        if (GraphenePlatform.isLinux()) {
            return new LinuxResolver();
        }

        if (GraphenePlatform.isWindows()) {
            return new WindowsResolver();
        }

        return new DefaultResolver();
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

    abstract class BaseResolver implements GrapheneKeyEventPlatformResolver {
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
    }

    final class DefaultResolver extends BaseResolver {
    }

    final class WindowsResolver extends BaseResolver {
        private static boolean isWindowsExtendedKey(int keyCode) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_RIGHT_ALT,
                     GLFW.GLFW_KEY_RIGHT_CONTROL,
                     GLFW.GLFW_KEY_INSERT,
                     GLFW.GLFW_KEY_DELETE,
                     GLFW.GLFW_KEY_HOME,
                     GLFW.GLFW_KEY_END,
                     GLFW.GLFW_KEY_PAGE_UP,
                     GLFW.GLFW_KEY_PAGE_DOWN,
                     GLFW.GLFW_KEY_UP,
                     GLFW.GLFW_KEY_DOWN,
                     GLFW.GLFW_KEY_LEFT,
                     GLFW.GLFW_KEY_RIGHT,
                     GLFW.GLFW_KEY_KP_ENTER,
                     GLFW.GLFW_KEY_KP_DIVIDE,
                     GLFW.GLFW_KEY_NUM_LOCK,
                     GLFW.GLFW_KEY_PRINT_SCREEN -> true;
                default -> false;
            };
        }

        @Override
        public int resolveWindowsKeyCode(int keyCode, int scanCode, char character, boolean numLockEnabled) {
            if (scanCode <= 0) {
                return super.resolveWindowsKeyCode(keyCode, scanCode, character, numLockEnabled);
            }

            int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
            if (GrapheneKeyCodeUtil.isNumpadKey(keyCode)) {
                if (GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) && !numLockEnabled) {
                    return 0;
                }

                return mappedKeyCode;
            }

            if (isLayoutDependentKey(keyCode)) {
                return 0;
            }

            return mappedKeyCode;
        }

        @Override
        public int resolveNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
            if (scanCode <= 0) {
                return 0;
            }

            return CefKeyEvent.buildWindowsNativeKeyCode(scanCode, isWindowsExtendedKey(keyCode), !pressed);
        }

        @Override
        public long resolveScanCode(int scanCode) {
            if (scanCode <= 0) {
                return 0L;
            }

            return scanCode & 0xFFL;
        }

        @Override
        public int resolveCharNativeKeyCode(char character) {
            return 0;
        }

        @Override
        public int sanitizeCharEventModifiers(int modifiers, boolean rightAltPressed) {
            boolean hasControlModifier = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
            boolean hasAltModifier = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
            if (rightAltPressed && hasControlModifier && hasAltModifier) {
                return modifiers & ~(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT);
            }

            return modifiers;
        }
    }

    final class LinuxResolver extends BaseResolver {
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
}
