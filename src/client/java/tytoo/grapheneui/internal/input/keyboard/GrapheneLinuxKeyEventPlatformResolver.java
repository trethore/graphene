package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;

final class GrapheneLinuxKeyEventPlatformResolver extends GrapheneBaseKeyEventPlatformResolver {
    private static final int XK_BACK_SPACE = 0xFF08;
    private static final int XK_TAB = 0xFF09;
    private static final int XK_RETURN = 0xFF0D;
    private static final int XK_ESCAPE = 0xFF1B;
    private static final int XK_LEFT = 0xFF51;
    private static final int XK_UP = 0xFF52;
    private static final int XK_RIGHT = 0xFF53;
    private static final int XK_DOWN = 0xFF54;
    private static final int XK_DELETE = 0xFFFF;

    private static boolean isPrintableCharacter(char character) {
        return character >= 0x20 && !Character.isISOControl(character);
    }

    private static boolean hasModifier(int modifiers, int modifier) {
        return (modifiers & modifier) != 0;
    }

    private static int resolveLinuxNativeKeyCode(int keyCode, char character) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> XK_BACK_SPACE;
            case GLFW.GLFW_KEY_DELETE -> XK_DELETE;
            case GLFW.GLFW_KEY_DOWN -> XK_DOWN;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> XK_RETURN;
            case GLFW.GLFW_KEY_ESCAPE -> XK_ESCAPE;
            case GLFW.GLFW_KEY_LEFT -> XK_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> XK_RIGHT;
            case GLFW.GLFW_KEY_TAB -> XK_TAB;
            case GLFW.GLFW_KEY_UP -> XK_UP;
            default -> isPrintableCharacter(character) ? character : 0;
        };
    }

    private static char resolveLinuxUnmodifiedCharacter(int keyCode, char character) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return '\r';
        }

        int nativeKeyCode = resolveLinuxNativeKeyCode(keyCode, character);
        if (nativeKeyCode != 0) {
            return (char) nativeKeyCode;
        }

        return character;
    }

    private static char getControlCharacter(int keyCode, boolean shift) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return (char) (keyCode - GLFW.GLFW_KEY_A + 1);
        }

        if (shift) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_2 -> 0;
                case GLFW.GLFW_KEY_6 -> 0x1E;
                case GLFW.GLFW_KEY_MINUS -> 0x1F;
                default -> 0;
            };
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_BRACKET -> 0x1B;
            case GLFW.GLFW_KEY_BACKSLASH -> 0x1C;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> 0x1D;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 0x0A;
            default -> 0;
        };
    }

    @Override
    public int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        int nativeKeyCode = resolveLinuxNativeKeyCode(keyCode, character);
        if (nativeKeyCode != 0) {
            return nativeKeyCode;
        }

        return super.getNativeKeyCode(keyCode, scanCode, character, pressed);
    }

    @Override
    public int getRawEventType(boolean pressed, int keyCode, char character) {
        if (!pressed) {
            return CefKeyEvent.KEYEVENT_KEYUP;
        }

        if (GrapheneDomKeyCodeMapper.isLayoutDependentKey(keyCode) && isPrintableCharacter(character)) {
            return CefKeyEvent.KEYEVENT_KEYDOWN;
        }

        return CefKeyEvent.KEYEVENT_RAWKEYDOWN;
    }

    @Override
    public boolean isSystemKey(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    @Override
    public char getRawEventUnmodifiedCharacter(int keyCode, char character, int modifiers) {
        return resolveLinuxUnmodifiedCharacter(keyCode, character);
    }

    @Override
    public char getRawEventCharacter(int keyCode, char unmodifiedCharacter, int modifiers) {
        if (hasModifier(modifiers, GLFW.GLFW_MOD_CONTROL)) {
            return getControlCharacter(keyCode, hasModifier(modifiers, GLFW.GLFW_MOD_SHIFT));
        }

        return unmodifiedCharacter;
    }

}
