package tytoo.grapheneui.internal.input.keyboard.mapping;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

final class GrapheneDomCodeMappings {
    private static final Object[][] DOM_CODE_FROM_GLFW = {
            {GLFW.GLFW_KEY_SPACE, "Space"},
            {GLFW.GLFW_KEY_APOSTROPHE, "Quote"},
            {GLFW.GLFW_KEY_COMMA, "Comma"},
            {GLFW.GLFW_KEY_MINUS, "Minus"},
            {GLFW.GLFW_KEY_PERIOD, "Period"},
            {GLFW.GLFW_KEY_SLASH, "Slash"},
            {GLFW.GLFW_KEY_0, "Digit0"},
            {GLFW.GLFW_KEY_1, "Digit1"},
            {GLFW.GLFW_KEY_2, "Digit2"},
            {GLFW.GLFW_KEY_3, "Digit3"},
            {GLFW.GLFW_KEY_4, "Digit4"},
            {GLFW.GLFW_KEY_5, "Digit5"},
            {GLFW.GLFW_KEY_6, "Digit6"},
            {GLFW.GLFW_KEY_7, "Digit7"},
            {GLFW.GLFW_KEY_8, "Digit8"},
            {GLFW.GLFW_KEY_9, "Digit9"},
            {GLFW.GLFW_KEY_SEMICOLON, "Semicolon"},
            {GLFW.GLFW_KEY_EQUAL, "Equal"},
            {GLFW.GLFW_KEY_A, "KeyA"},
            {GLFW.GLFW_KEY_B, "KeyB"},
            {GLFW.GLFW_KEY_C, "KeyC"},
            {GLFW.GLFW_KEY_D, "KeyD"},
            {GLFW.GLFW_KEY_E, "KeyE"},
            {GLFW.GLFW_KEY_F, "KeyF"},
            {GLFW.GLFW_KEY_G, "KeyG"},
            {GLFW.GLFW_KEY_H, "KeyH"},
            {GLFW.GLFW_KEY_I, "KeyI"},
            {GLFW.GLFW_KEY_J, "KeyJ"},
            {GLFW.GLFW_KEY_K, "KeyK"},
            {GLFW.GLFW_KEY_L, "KeyL"},
            {GLFW.GLFW_KEY_M, "KeyM"},
            {GLFW.GLFW_KEY_N, "KeyN"},
            {GLFW.GLFW_KEY_O, "KeyO"},
            {GLFW.GLFW_KEY_P, "KeyP"},
            {GLFW.GLFW_KEY_Q, "KeyQ"},
            {GLFW.GLFW_KEY_R, "KeyR"},
            {GLFW.GLFW_KEY_S, "KeyS"},
            {GLFW.GLFW_KEY_T, "KeyT"},
            {GLFW.GLFW_KEY_U, "KeyU"},
            {GLFW.GLFW_KEY_V, "KeyV"},
            {GLFW.GLFW_KEY_W, "KeyW"},
            {GLFW.GLFW_KEY_X, "KeyX"},
            {GLFW.GLFW_KEY_Y, "KeyY"},
            {GLFW.GLFW_KEY_Z, "KeyZ"},
            {GLFW.GLFW_KEY_LEFT_BRACKET, "BracketLeft"},
            {GLFW.GLFW_KEY_BACKSLASH, "Backslash"},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, "BracketRight"},
            {GLFW.GLFW_KEY_GRAVE_ACCENT, "Backquote"},
            {GLFW.GLFW_KEY_WORLD_1, "IntlBackslash"},
            {GLFW.GLFW_KEY_WORLD_2, "IntlRo"},
            {GLFW.GLFW_KEY_ESCAPE, "Escape"},
            {GLFW.GLFW_KEY_ENTER, "Enter"},
            {GLFW.GLFW_KEY_TAB, "Tab"},
            {GLFW.GLFW_KEY_BACKSPACE, "Backspace"},
            {GLFW.GLFW_KEY_INSERT, "Insert"},
            {GLFW.GLFW_KEY_DELETE, "Delete"},
            {GLFW.GLFW_KEY_RIGHT, "ArrowRight"},
            {GLFW.GLFW_KEY_LEFT, "ArrowLeft"},
            {GLFW.GLFW_KEY_DOWN, "ArrowDown"},
            {GLFW.GLFW_KEY_UP, "ArrowUp"},
            {GLFW.GLFW_KEY_PAGE_UP, "PageUp"},
            {GLFW.GLFW_KEY_PAGE_DOWN, "PageDown"},
            {GLFW.GLFW_KEY_HOME, "Home"},
            {GLFW.GLFW_KEY_END, "End"},
            {GLFW.GLFW_KEY_CAPS_LOCK, "CapsLock"},
            {GLFW.GLFW_KEY_SCROLL_LOCK, "ScrollLock"},
            {GLFW.GLFW_KEY_NUM_LOCK, "NumLock"},
            {GLFW.GLFW_KEY_PRINT_SCREEN, "PrintScreen"},
            {GLFW.GLFW_KEY_PAUSE, "Pause"},
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
            {GLFW.GLFW_KEY_F25, "F25"},
            {GLFW.GLFW_KEY_KP_0, "Numpad0"},
            {GLFW.GLFW_KEY_KP_1, "Numpad1"},
            {GLFW.GLFW_KEY_KP_2, "Numpad2"},
            {GLFW.GLFW_KEY_KP_3, "Numpad3"},
            {GLFW.GLFW_KEY_KP_4, "Numpad4"},
            {GLFW.GLFW_KEY_KP_5, "Numpad5"},
            {GLFW.GLFW_KEY_KP_6, "Numpad6"},
            {GLFW.GLFW_KEY_KP_7, "Numpad7"},
            {GLFW.GLFW_KEY_KP_8, "Numpad8"},
            {GLFW.GLFW_KEY_KP_9, "Numpad9"},
            {GLFW.GLFW_KEY_KP_DECIMAL, "NumpadDecimal"},
            {GLFW.GLFW_KEY_KP_DIVIDE, "NumpadDivide"},
            {GLFW.GLFW_KEY_KP_MULTIPLY, "NumpadMultiply"},
            {GLFW.GLFW_KEY_KP_SUBTRACT, "NumpadSubtract"},
            {GLFW.GLFW_KEY_KP_ADD, "NumpadAdd"},
            {GLFW.GLFW_KEY_KP_ENTER, "NumpadEnter"},
            {GLFW.GLFW_KEY_KP_EQUAL, "NumpadEqual"},
            {GLFW.GLFW_KEY_LEFT_SHIFT, "ShiftLeft"},
            {GLFW.GLFW_KEY_LEFT_CONTROL, "ControlLeft"},
            {GLFW.GLFW_KEY_LEFT_ALT, "AltLeft"},
            {GLFW.GLFW_KEY_LEFT_SUPER, "MetaLeft"},
            {GLFW.GLFW_KEY_RIGHT_SHIFT, "ShiftRight"},
            {GLFW.GLFW_KEY_RIGHT_CONTROL, "ControlRight"},
            {GLFW.GLFW_KEY_RIGHT_ALT, "AltRight"},
            {GLFW.GLFW_KEY_RIGHT_SUPER, "MetaRight"},
            {GLFW.GLFW_KEY_MENU, "ContextMenu"}
    };
    private static final Map<Integer, String> DOM_CODE_BY_GLFW = createDomCodeByFirstColumn();

    private GrapheneDomCodeMappings() {
    }

    static String fromGlfw(int keyCode) {
        return DOM_CODE_BY_GLFW.getOrDefault(keyCode, "");
    }

    private static Map<Integer, String> createDomCodeByFirstColumn() {
        Map<Integer, String> mappings = new HashMap<>();
        for (Object[] row : DOM_CODE_FROM_GLFW) {
            mappings.put((Integer) row[0], (String) row[1]);
        }

        return Map.copyOf(mappings);
    }
}
