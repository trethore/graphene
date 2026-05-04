package tytoo.grapheneui.internal.input.keyboard.mapping;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

final class GrapheneWindowsVirtualKeyMappings {
    private static final int AWT_VK_RETURN = 0x0D;
    private static final int WINDOWS_VK_OEM_1 = 0xBA;
    private static final int WINDOWS_VK_OEM_PLUS = 0xBB;
    private static final int WINDOWS_VK_OEM_COMMA = 0xBC;
    private static final int WINDOWS_VK_OEM_MINUS = 0xBD;
    private static final int WINDOWS_VK_OEM_PERIOD = 0xBE;
    private static final int WINDOWS_VK_OEM_2 = 0xBF;
    private static final int WINDOWS_VK_OEM_3 = 0xC0;
    private static final int WINDOWS_VK_OEM_4 = 0xDB;
    private static final int WINDOWS_VK_OEM_5 = 0xDC;
    private static final int WINDOWS_VK_OEM_6 = 0xDD;
    private static final int WINDOWS_VK_OEM_7 = 0xDE;
    private static final int WINDOWS_VK_OEM_8 = 0xDF;
    private static final int WINDOWS_VK_OEM_102 = 0xE2;
    private static final int WINDOWS_VK_LWIN = 0x5B;
    private static final int WINDOWS_VK_RWIN = 0x5C;

    private static final int[][] WINDOWS_VK_FROM_GLFW = {
            {GLFW.GLFW_KEY_BACKSPACE, KeyEvent.VK_BACK_SPACE},
            {GLFW.GLFW_KEY_TAB, KeyEvent.VK_TAB},
            {GLFW.GLFW_KEY_ENTER, AWT_VK_RETURN},
            {GLFW.GLFW_KEY_KP_ENTER, AWT_VK_RETURN},
            {GLFW.GLFW_KEY_0, KeyEvent.VK_0},
            {GLFW.GLFW_KEY_1, KeyEvent.VK_1},
            {GLFW.GLFW_KEY_2, KeyEvent.VK_2},
            {GLFW.GLFW_KEY_3, KeyEvent.VK_3},
            {GLFW.GLFW_KEY_4, KeyEvent.VK_4},
            {GLFW.GLFW_KEY_5, KeyEvent.VK_5},
            {GLFW.GLFW_KEY_6, KeyEvent.VK_6},
            {GLFW.GLFW_KEY_7, KeyEvent.VK_7},
            {GLFW.GLFW_KEY_8, KeyEvent.VK_8},
            {GLFW.GLFW_KEY_9, KeyEvent.VK_9},
            {GLFW.GLFW_KEY_LEFT_SHIFT, KeyEvent.VK_SHIFT},
            {GLFW.GLFW_KEY_RIGHT_SHIFT, KeyEvent.VK_SHIFT},
            {GLFW.GLFW_KEY_LEFT_CONTROL, KeyEvent.VK_CONTROL},
            {GLFW.GLFW_KEY_RIGHT_CONTROL, KeyEvent.VK_CONTROL},
            {GLFW.GLFW_KEY_LEFT_ALT, KeyEvent.VK_ALT},
            {GLFW.GLFW_KEY_RIGHT_ALT, KeyEvent.VK_ALT},
            {GLFW.GLFW_KEY_LEFT_SUPER, WINDOWS_VK_LWIN},
            {GLFW.GLFW_KEY_RIGHT_SUPER, WINDOWS_VK_RWIN},
            {GLFW.GLFW_KEY_GRAVE_ACCENT, WINDOWS_VK_OEM_3},
            {GLFW.GLFW_KEY_MINUS, WINDOWS_VK_OEM_MINUS},
            {GLFW.GLFW_KEY_EQUAL, WINDOWS_VK_OEM_PLUS},
            {GLFW.GLFW_KEY_LEFT_BRACKET, WINDOWS_VK_OEM_4},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, WINDOWS_VK_OEM_6},
            {GLFW.GLFW_KEY_BACKSLASH, WINDOWS_VK_OEM_5},
            {GLFW.GLFW_KEY_SEMICOLON, WINDOWS_VK_OEM_1},
            {GLFW.GLFW_KEY_APOSTROPHE, WINDOWS_VK_OEM_7},
            {GLFW.GLFW_KEY_COMMA, WINDOWS_VK_OEM_COMMA},
            {GLFW.GLFW_KEY_PERIOD, WINDOWS_VK_OEM_PERIOD},
            {GLFW.GLFW_KEY_SLASH, WINDOWS_VK_OEM_2},
            {GLFW.GLFW_KEY_WORLD_1, WINDOWS_VK_OEM_102},
            {GLFW.GLFW_KEY_WORLD_2, WINDOWS_VK_OEM_102},
            {GLFW.GLFW_KEY_ESCAPE, KeyEvent.VK_ESCAPE},
            {GLFW.GLFW_KEY_SPACE, KeyEvent.VK_SPACE},
            {GLFW.GLFW_KEY_INSERT, KeyEvent.VK_INSERT},
            {GLFW.GLFW_KEY_LEFT, KeyEvent.VK_LEFT},
            {GLFW.GLFW_KEY_RIGHT, KeyEvent.VK_RIGHT},
            {GLFW.GLFW_KEY_UP, KeyEvent.VK_UP},
            {GLFW.GLFW_KEY_DOWN, KeyEvent.VK_DOWN},
            {GLFW.GLFW_KEY_CAPS_LOCK, KeyEvent.VK_CAPS_LOCK},
            {GLFW.GLFW_KEY_SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK},
            {GLFW.GLFW_KEY_NUM_LOCK, KeyEvent.VK_NUM_LOCK},
            {GLFW.GLFW_KEY_PRINT_SCREEN, KeyEvent.VK_PRINTSCREEN},
            {GLFW.GLFW_KEY_PAUSE, KeyEvent.VK_PAUSE},
            {GLFW.GLFW_KEY_DELETE, KeyEvent.VK_DELETE},
            {GLFW.GLFW_KEY_HOME, KeyEvent.VK_HOME},
            {GLFW.GLFW_KEY_END, KeyEvent.VK_END},
            {GLFW.GLFW_KEY_PAGE_UP, KeyEvent.VK_PAGE_UP},
            {GLFW.GLFW_KEY_PAGE_DOWN, KeyEvent.VK_PAGE_DOWN},
            {GLFW.GLFW_KEY_F1, KeyEvent.VK_F1},
            {GLFW.GLFW_KEY_F2, KeyEvent.VK_F2},
            {GLFW.GLFW_KEY_F3, KeyEvent.VK_F3},
            {GLFW.GLFW_KEY_F4, KeyEvent.VK_F4},
            {GLFW.GLFW_KEY_F5, KeyEvent.VK_F5},
            {GLFW.GLFW_KEY_F6, KeyEvent.VK_F6},
            {GLFW.GLFW_KEY_F7, KeyEvent.VK_F7},
            {GLFW.GLFW_KEY_F8, KeyEvent.VK_F8},
            {GLFW.GLFW_KEY_F9, KeyEvent.VK_F9},
            {GLFW.GLFW_KEY_F10, KeyEvent.VK_F10},
            {GLFW.GLFW_KEY_F11, KeyEvent.VK_F11},
            {GLFW.GLFW_KEY_F12, KeyEvent.VK_F12},
            {GLFW.GLFW_KEY_KP_0, KeyEvent.VK_NUMPAD0},
            {GLFW.GLFW_KEY_KP_1, KeyEvent.VK_NUMPAD1},
            {GLFW.GLFW_KEY_KP_2, KeyEvent.VK_NUMPAD2},
            {GLFW.GLFW_KEY_KP_3, KeyEvent.VK_NUMPAD3},
            {GLFW.GLFW_KEY_KP_4, KeyEvent.VK_NUMPAD4},
            {GLFW.GLFW_KEY_KP_5, KeyEvent.VK_NUMPAD5},
            {GLFW.GLFW_KEY_KP_6, KeyEvent.VK_NUMPAD6},
            {GLFW.GLFW_KEY_KP_7, KeyEvent.VK_NUMPAD7},
            {GLFW.GLFW_KEY_KP_8, KeyEvent.VK_NUMPAD8},
            {GLFW.GLFW_KEY_KP_9, KeyEvent.VK_NUMPAD9},
            {GLFW.GLFW_KEY_KP_DECIMAL, KeyEvent.VK_DECIMAL},
            {GLFW.GLFW_KEY_KP_DIVIDE, KeyEvent.VK_DIVIDE},
            {GLFW.GLFW_KEY_KP_MULTIPLY, KeyEvent.VK_MULTIPLY},
            {GLFW.GLFW_KEY_KP_SUBTRACT, KeyEvent.VK_SUBTRACT},
            {GLFW.GLFW_KEY_KP_ADD, KeyEvent.VK_ADD},
            {GLFW.GLFW_KEY_KP_EQUAL, KeyEvent.VK_EQUALS}
    };
    private static final Map<Integer, Integer> WINDOWS_VK_BY_GLFW = createByFirstColumn(WINDOWS_VK_FROM_GLFW);
    private static final int[][] WINDOWS_VK_FROM_CHARACTER = {
            {'0', KeyEvent.VK_0},
            {')', KeyEvent.VK_0},
            {'1', KeyEvent.VK_1},
            {'!', KeyEvent.VK_1},
            {'2', KeyEvent.VK_2},
            {'@', KeyEvent.VK_2},
            {'3', KeyEvent.VK_3},
            {'#', KeyEvent.VK_3},
            {'4', KeyEvent.VK_4},
            {'$', KeyEvent.VK_4},
            {'5', KeyEvent.VK_5},
            {'%', KeyEvent.VK_5},
            {'6', KeyEvent.VK_6},
            {'^', KeyEvent.VK_6},
            {'7', KeyEvent.VK_7},
            {'&', KeyEvent.VK_7},
            {'8', KeyEvent.VK_8},
            {'*', KeyEvent.VK_8},
            {'9', KeyEvent.VK_9},
            {'(', KeyEvent.VK_9},
            {'`', WINDOWS_VK_OEM_3},
            {'~', WINDOWS_VK_OEM_3},
            {'-', WINDOWS_VK_OEM_MINUS},
            {'_', WINDOWS_VK_OEM_MINUS},
            {'=', WINDOWS_VK_OEM_PLUS},
            {'+', WINDOWS_VK_OEM_PLUS},
            {'[', WINDOWS_VK_OEM_4},
            {'{', WINDOWS_VK_OEM_4},
            {']', WINDOWS_VK_OEM_6},
            {'}', WINDOWS_VK_OEM_6},
            {'\\', WINDOWS_VK_OEM_5},
            {'|', WINDOWS_VK_OEM_5},
            {';', WINDOWS_VK_OEM_1},
            {':', WINDOWS_VK_OEM_1},
            {'\'', WINDOWS_VK_OEM_7},
            {'"', WINDOWS_VK_OEM_7},
            {',', WINDOWS_VK_OEM_COMMA},
            {'<', WINDOWS_VK_OEM_COMMA},
            {'.', WINDOWS_VK_OEM_PERIOD},
            {'>', WINDOWS_VK_OEM_PERIOD},
            {'/', WINDOWS_VK_OEM_2},
            {'?', WINDOWS_VK_OEM_2},
            {'\u00E9', KeyEvent.VK_2},
            {'\u00C9', KeyEvent.VK_2},
            {'\u00E8', KeyEvent.VK_7},
            {'\u00C8', KeyEvent.VK_7},
            {'\u00E7', KeyEvent.VK_9},
            {'\u00C7', KeyEvent.VK_9},
            {'\u00E0', KeyEvent.VK_0},
            {'\u00C0', KeyEvent.VK_0},
            {'\u00F9', WINDOWS_VK_OEM_3},
            {'\u00D9', WINDOWS_VK_OEM_3},
            {'\u00B2', WINDOWS_VK_OEM_7}
    };
    private static final Map<Integer, Integer> WINDOWS_VK_BY_CHARACTER = createByFirstColumn(WINDOWS_VK_FROM_CHARACTER);
    private static final int[][] WINDOWS_VK_LAYOUT_OVERRIDES = {
            {GLFW.GLFW_KEY_COMMA, ';', WINDOWS_VK_OEM_PERIOD},
            {GLFW.GLFW_KEY_PERIOD, ':', WINDOWS_VK_OEM_2},
            {GLFW.GLFW_KEY_SLASH, '!', WINDOWS_VK_OEM_8},
            {GLFW.GLFW_KEY_APOSTROPHE, '%', WINDOWS_VK_OEM_3},
            {GLFW.GLFW_KEY_APOSTROPHE, '\u00F9', WINDOWS_VK_OEM_3},
            {GLFW.GLFW_KEY_APOSTROPHE, '\u00D9', WINDOWS_VK_OEM_3},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, '$', WINDOWS_VK_OEM_1},
            {GLFW.GLFW_KEY_LEFT_BRACKET, '^', WINDOWS_VK_OEM_6},
            {GLFW.GLFW_KEY_GRAVE_ACCENT, '\u00B2', WINDOWS_VK_OEM_7}
    };
    private static final Map<Long, Integer> WINDOWS_VK_LAYOUT_OVERRIDES_BY_PAIR = createByKeyAndCharacter();

    private GrapheneWindowsVirtualKeyMappings() {
    }

    static int fromGlfw(int keyCode) {
        return WINDOWS_VK_BY_GLFW.getOrDefault(keyCode, 0);
    }

    static int fromCharacter(char character) {
        return WINDOWS_VK_BY_CHARACTER.getOrDefault((int) character, 0);
    }

    static int fromLayoutPair(int keyCode, char character) {
        return WINDOWS_VK_LAYOUT_OVERRIDES_BY_PAIR.getOrDefault(pairKey(keyCode, character), 0);
    }

    private static Map<Integer, Integer> createByFirstColumn(int[][] mappingRows) {
        Map<Integer, Integer> mappings = new HashMap<>();
        for (int[] mappingRow : mappingRows) {
            mappings.put(mappingRow[0], mappingRow[1]);
        }

        return Map.copyOf(mappings);
    }

    private static Map<Long, Integer> createByKeyAndCharacter() {
        Map<Long, Integer> mappings = new HashMap<>();
        for (int[] row : WINDOWS_VK_LAYOUT_OVERRIDES) {
            mappings.put(pairKey(row[0], (char) row[1]), row[2]);
        }

        return Map.copyOf(mappings);
    }

    private static long pairKey(int keyCode, char character) {
        return ((long) keyCode << 32) | (character & 0xFFFFL);
    }
}
