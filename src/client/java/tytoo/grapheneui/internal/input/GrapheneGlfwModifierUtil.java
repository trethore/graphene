package tytoo.grapheneui.internal.input;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

public final class GrapheneGlfwModifierUtil {
    private GrapheneGlfwModifierUtil() {
    }

    public static int currentModifiers() {
        long windowHandle = GLFW.glfwGetCurrentContext();
        if (windowHandle == 0L) {
            return 0;
        }

        int modifiers = 0;
        if (isEitherKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }

        if (isEitherKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }

        if (isEitherKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT)) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }

        if (isEitherKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER)) {
            modifiers |= GLFW.GLFW_MOD_SUPER;
        }

        return modifiers;
    }

    public static boolean isEditShortcutModifierDown(int modifiers) {
        if (GraphenePlatform.isMac()) {
            return (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
        }

        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    private static boolean isEitherKeyDown(long windowHandle, int leftKey, int rightKey) {
        return isKeyDown(windowHandle, leftKey) || isKeyDown(windowHandle, rightKey);
    }

    private static boolean isKeyDown(long windowHandle, int key) {
        int keyState = GLFW.glfwGetKey(windowHandle, key);
        return keyState == GLFW.GLFW_PRESS || keyState == GLFW.GLFW_REPEAT;
    }
}
