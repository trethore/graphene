package tytoo.grapheneui.internal.input.keyboard.mapping;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

final class GrapheneDomKeyMappings {
    private static final Object[][] DOM_KEY_FROM_GLFW = {
            {GLFW.GLFW_KEY_BACKSPACE, "Backspace"},
            {GLFW.GLFW_KEY_TAB, "Tab"},
            {GLFW.GLFW_KEY_ENTER, "Enter"},
            {GLFW.GLFW_KEY_KP_ENTER, "Enter"},
            {GLFW.GLFW_KEY_LEFT_SHIFT, "Shift"},
            {GLFW.GLFW_KEY_RIGHT_SHIFT, "Shift"},
            {GLFW.GLFW_KEY_LEFT_CONTROL, "Control"},
            {GLFW.GLFW_KEY_RIGHT_CONTROL, "Control"},
            {GLFW.GLFW_KEY_LEFT_ALT, "Alt"},
            {GLFW.GLFW_KEY_RIGHT_ALT, "Alt"},
            {GLFW.GLFW_KEY_LEFT_SUPER, "Meta"},
            {GLFW.GLFW_KEY_RIGHT_SUPER, "Meta"},
            {GLFW.GLFW_KEY_ESCAPE, "Escape"},
            {GLFW.GLFW_KEY_SPACE, " "},
            {GLFW.GLFW_KEY_INSERT, "Insert"},
            {GLFW.GLFW_KEY_DELETE, "Delete"},
            {GLFW.GLFW_KEY_LEFT, "ArrowLeft"},
            {GLFW.GLFW_KEY_RIGHT, "ArrowRight"},
            {GLFW.GLFW_KEY_UP, "ArrowUp"},
            {GLFW.GLFW_KEY_DOWN, "ArrowDown"},
            {GLFW.GLFW_KEY_HOME, "Home"},
            {GLFW.GLFW_KEY_END, "End"},
            {GLFW.GLFW_KEY_PAGE_UP, "PageUp"},
            {GLFW.GLFW_KEY_PAGE_DOWN, "PageDown"},
            {GLFW.GLFW_KEY_CAPS_LOCK, "CapsLock"},
            {GLFW.GLFW_KEY_SCROLL_LOCK, "ScrollLock"},
            {GLFW.GLFW_KEY_NUM_LOCK, "NumLock"},
            {GLFW.GLFW_KEY_PRINT_SCREEN, "PrintScreen"},
            {GLFW.GLFW_KEY_PAUSE, "Pause"},
            {GLFW.GLFW_KEY_MENU, "ContextMenu"},
            {GLFW.GLFW_KEY_F1, "F1"},
            {GLFW.GLFW_KEY_F2, "F2"},
            {GLFW.GLFW_KEY_F3, "F3"},
            {GLFW.GLFW_KEY_F4, "F4"},
            {GLFW.GLFW_KEY_F5, "F5"},
            {GLFW.GLFW_KEY_F6, "F6"},
            {GLFW.GLFW_KEY_F7, "F7"},
            {GLFW.GLFW_KEY_F8, "F8"},
            {GLFW.GLFW_KEY_F9, "F9"},
            {GLFW.GLFW_KEY_F10, "F10"},
            {GLFW.GLFW_KEY_F11, "F11"},
            {GLFW.GLFW_KEY_F12, "F12"},
            {GLFW.GLFW_KEY_F13, "F13"},
            {GLFW.GLFW_KEY_F14, "F14"},
            {GLFW.GLFW_KEY_F15, "F15"},
            {GLFW.GLFW_KEY_F16, "F16"},
            {GLFW.GLFW_KEY_F17, "F17"},
            {GLFW.GLFW_KEY_F18, "F18"},
            {GLFW.GLFW_KEY_F19, "F19"},
            {GLFW.GLFW_KEY_F20, "F20"},
            {GLFW.GLFW_KEY_F21, "F21"},
            {GLFW.GLFW_KEY_F22, "F22"},
            {GLFW.GLFW_KEY_F23, "F23"},
            {GLFW.GLFW_KEY_F24, "F24"},
            {GLFW.GLFW_KEY_F25, "F25"}
    };
    private static final Map<Integer, String> DOM_KEY_BY_GLFW = createDomKeyByFirstColumn();

    private GrapheneDomKeyMappings() {
    }

    static String fromGlfw(int keyCode) {
        return DOM_KEY_BY_GLFW.getOrDefault(keyCode, "");
    }

    private static Map<Integer, String> createDomKeyByFirstColumn() {
        Map<Integer, String> mappings = new HashMap<>();
        for (Object[] row : DOM_KEY_FROM_GLFW) {
            mappings.put((Integer) row[0], (String) row[1]);
        }

        return Map.copyOf(mappings);
    }
}
