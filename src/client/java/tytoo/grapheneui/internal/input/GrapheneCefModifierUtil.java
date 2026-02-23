package tytoo.grapheneui.internal.input;

import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;

public final class GrapheneCefModifierUtil {
    private GrapheneCefModifierUtil() {
    }

    public static int toCefCommonModifiers(int modifiers) {
        int cefModifiers = EventFlags.EVENTFLAG_NONE;
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_SHIFT_DOWN;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_CONTROL_DOWN;
        }

        if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_ALT_DOWN;
        }

        if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_COMMAND_DOWN;
        }

        return cefModifiers;
    }
}
