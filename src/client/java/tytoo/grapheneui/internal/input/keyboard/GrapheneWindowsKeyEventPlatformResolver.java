package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;

final class GrapheneWindowsKeyEventPlatformResolver extends GrapheneBaseKeyEventPlatformResolver {
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
    public int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (scanCode <= 0) {
            return 0;
        }

        return CefKeyEvent.buildWindowsNativeKeyCode(scanCode, isWindowsExtendedKey(keyCode), !pressed);
    }

    @Override
    public long getScanCode(int scanCode) {
        if (scanCode <= 0) {
            return 0L;
        }

        return scanCode & 0xFFL;
    }

    @Override
    public int getCharNativeKeyCode(char character) {
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
