package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class GrapheneKeyboardMappings {
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
            {GLFW.GLFW_KEY_LEFT_SUPER, KeyEvent.VK_WINDOWS},
            {GLFW.GLFW_KEY_RIGHT_SUPER, KeyEvent.VK_WINDOWS},
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
    private static final Map<Integer, Integer> WINDOWS_VK_BY_GLFW = createByFirstColumn(WINDOWS_VK_FROM_GLFW, 1);
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

    private static final Map<Integer, Integer> WINDOWS_VK_BY_CHARACTER = createByFirstColumn(WINDOWS_VK_FROM_CHARACTER, 1);
    private static final Map<Long, Integer> WINDOWS_VK_LAYOUT_OVERRIDES_BY_PAIR = createByKeyAndCharacter(WINDOWS_VK_LAYOUT_OVERRIDES);
    private static final int[][] GLFW_KEY_TO_CHARACTER = {
            {GLFW.GLFW_KEY_A, 'a', 'A'},
            {GLFW.GLFW_KEY_B, 'b', 'B'},
            {GLFW.GLFW_KEY_C, 'c', 'C'},
            {GLFW.GLFW_KEY_D, 'd', 'D'},
            {GLFW.GLFW_KEY_E, 'e', 'E'},
            {GLFW.GLFW_KEY_F, 'f', 'F'},
            {GLFW.GLFW_KEY_G, 'g', 'G'},
            {GLFW.GLFW_KEY_H, 'h', 'H'},
            {GLFW.GLFW_KEY_I, 'i', 'I'},
            {GLFW.GLFW_KEY_J, 'j', 'J'},
            {GLFW.GLFW_KEY_K, 'k', 'K'},
            {GLFW.GLFW_KEY_L, 'l', 'L'},
            {GLFW.GLFW_KEY_M, 'm', 'M'},
            {GLFW.GLFW_KEY_N, 'n', 'N'},
            {GLFW.GLFW_KEY_O, 'o', 'O'},
            {GLFW.GLFW_KEY_P, 'p', 'P'},
            {GLFW.GLFW_KEY_Q, 'q', 'Q'},
            {GLFW.GLFW_KEY_R, 'r', 'R'},
            {GLFW.GLFW_KEY_S, 's', 'S'},
            {GLFW.GLFW_KEY_T, 't', 'T'},
            {GLFW.GLFW_KEY_U, 'u', 'U'},
            {GLFW.GLFW_KEY_V, 'v', 'V'},
            {GLFW.GLFW_KEY_W, 'w', 'W'},
            {GLFW.GLFW_KEY_X, 'x', 'X'},
            {GLFW.GLFW_KEY_Y, 'y', 'Y'},
            {GLFW.GLFW_KEY_Z, 'z', 'Z'},
            {GLFW.GLFW_KEY_0, '0', ')'},
            {GLFW.GLFW_KEY_1, '1', '!'},
            {GLFW.GLFW_KEY_2, '2', '@'},
            {GLFW.GLFW_KEY_3, '3', '#'},
            {GLFW.GLFW_KEY_4, '4', '$'},
            {GLFW.GLFW_KEY_5, '5', '%'},
            {GLFW.GLFW_KEY_6, '6', '^'},
            {GLFW.GLFW_KEY_7, '7', '&'},
            {GLFW.GLFW_KEY_8, '8', '*'},
            {GLFW.GLFW_KEY_9, '9', '('},
            {GLFW.GLFW_KEY_KP_0, '0', '0'},
            {GLFW.GLFW_KEY_KP_1, '1', '1'},
            {GLFW.GLFW_KEY_KP_2, '2', '2'},
            {GLFW.GLFW_KEY_KP_3, '3', '3'},
            {GLFW.GLFW_KEY_KP_4, '4', '4'},
            {GLFW.GLFW_KEY_KP_5, '5', '5'},
            {GLFW.GLFW_KEY_KP_6, '6', '6'},
            {GLFW.GLFW_KEY_KP_7, '7', '7'},
            {GLFW.GLFW_KEY_KP_8, '8', '8'},
            {GLFW.GLFW_KEY_KP_9, '9', '9'},
            {GLFW.GLFW_KEY_MINUS, '-', '_'},
            {GLFW.GLFW_KEY_EQUAL, '=', '+'},
            {GLFW.GLFW_KEY_KP_DECIMAL, '.', '.'},
            {GLFW.GLFW_KEY_KP_DIVIDE, '/', '/'},
            {GLFW.GLFW_KEY_KP_MULTIPLY, '*', '*'},
            {GLFW.GLFW_KEY_KP_SUBTRACT, '-', '-'},
            {GLFW.GLFW_KEY_KP_ADD, '+', '+'},
            {GLFW.GLFW_KEY_KP_EQUAL, '=', '='},
            {GLFW.GLFW_KEY_BACKSLASH, '\\', '|'},
            {GLFW.GLFW_KEY_SLASH, '/', '?'},
            {GLFW.GLFW_KEY_SEMICOLON, ';', ':'},
            {GLFW.GLFW_KEY_COMMA, ',', '<'},
            {GLFW.GLFW_KEY_PERIOD, '.', '>'},
            {GLFW.GLFW_KEY_APOSTROPHE, '\'', '"'},
            {GLFW.GLFW_KEY_LEFT_BRACKET, '[', '{'},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, ']', '}'},
            {GLFW.GLFW_KEY_TAB, '\t', '\t'},
            {GLFW.GLFW_KEY_ENTER, '\r', '\r'},
            {GLFW.GLFW_KEY_KP_ENTER, '\r', '\r'},
            {GLFW.GLFW_KEY_BACKSPACE, '\b', '\b'},
            {GLFW.GLFW_KEY_SPACE, ' ', ' '}
    };
    private static final int[][] NUMPAD_KEYS = {
            {GLFW.GLFW_KEY_KP_0},
            {GLFW.GLFW_KEY_KP_1},
            {GLFW.GLFW_KEY_KP_2},
            {GLFW.GLFW_KEY_KP_3},
            {GLFW.GLFW_KEY_KP_4},
            {GLFW.GLFW_KEY_KP_5},
            {GLFW.GLFW_KEY_KP_6},
            {GLFW.GLFW_KEY_KP_7},
            {GLFW.GLFW_KEY_KP_8},
            {GLFW.GLFW_KEY_KP_9},
            {GLFW.GLFW_KEY_KP_DECIMAL},
            {GLFW.GLFW_KEY_KP_DIVIDE},
            {GLFW.GLFW_KEY_KP_MULTIPLY},
            {GLFW.GLFW_KEY_KP_SUBTRACT},
            {GLFW.GLFW_KEY_KP_ADD},
            {GLFW.GLFW_KEY_KP_ENTER},
            {GLFW.GLFW_KEY_KP_EQUAL}
    };
    private static final int[][] NUMPAD_TEXT_KEYS = {
            {GLFW.GLFW_KEY_KP_0},
            {GLFW.GLFW_KEY_KP_1},
            {GLFW.GLFW_KEY_KP_2},
            {GLFW.GLFW_KEY_KP_3},
            {GLFW.GLFW_KEY_KP_4},
            {GLFW.GLFW_KEY_KP_5},
            {GLFW.GLFW_KEY_KP_6},
            {GLFW.GLFW_KEY_KP_7},
            {GLFW.GLFW_KEY_KP_8},
            {GLFW.GLFW_KEY_KP_9},
            {GLFW.GLFW_KEY_KP_DECIMAL},
            {GLFW.GLFW_KEY_KP_DIVIDE},
            {GLFW.GLFW_KEY_KP_MULTIPLY},
            {GLFW.GLFW_KEY_KP_SUBTRACT},
            {GLFW.GLFW_KEY_KP_ADD},
            {GLFW.GLFW_KEY_KP_EQUAL}
    };
    private static final int[][] NUMPAD_TEXT_REQUIRING_NUM_LOCK = {
            {GLFW.GLFW_KEY_KP_0},
            {GLFW.GLFW_KEY_KP_1},
            {GLFW.GLFW_KEY_KP_2},
            {GLFW.GLFW_KEY_KP_3},
            {GLFW.GLFW_KEY_KP_4},
            {GLFW.GLFW_KEY_KP_5},
            {GLFW.GLFW_KEY_KP_6},
            {GLFW.GLFW_KEY_KP_7},
            {GLFW.GLFW_KEY_KP_8},
            {GLFW.GLFW_KEY_KP_9},
            {GLFW.GLFW_KEY_KP_DECIMAL}
    };
    private static final int[][] LAYOUT_DEPENDENT_KEYS = {
            {GLFW.GLFW_KEY_A},
            {GLFW.GLFW_KEY_B},
            {GLFW.GLFW_KEY_C},
            {GLFW.GLFW_KEY_D},
            {GLFW.GLFW_KEY_E},
            {GLFW.GLFW_KEY_F},
            {GLFW.GLFW_KEY_G},
            {GLFW.GLFW_KEY_H},
            {GLFW.GLFW_KEY_I},
            {GLFW.GLFW_KEY_J},
            {GLFW.GLFW_KEY_K},
            {GLFW.GLFW_KEY_L},
            {GLFW.GLFW_KEY_M},
            {GLFW.GLFW_KEY_N},
            {GLFW.GLFW_KEY_O},
            {GLFW.GLFW_KEY_P},
            {GLFW.GLFW_KEY_Q},
            {GLFW.GLFW_KEY_R},
            {GLFW.GLFW_KEY_S},
            {GLFW.GLFW_KEY_T},
            {GLFW.GLFW_KEY_U},
            {GLFW.GLFW_KEY_V},
            {GLFW.GLFW_KEY_W},
            {GLFW.GLFW_KEY_X},
            {GLFW.GLFW_KEY_Y},
            {GLFW.GLFW_KEY_Z},
            {GLFW.GLFW_KEY_0},
            {GLFW.GLFW_KEY_1},
            {GLFW.GLFW_KEY_2},
            {GLFW.GLFW_KEY_3},
            {GLFW.GLFW_KEY_4},
            {GLFW.GLFW_KEY_5},
            {GLFW.GLFW_KEY_6},
            {GLFW.GLFW_KEY_7},
            {GLFW.GLFW_KEY_8},
            {GLFW.GLFW_KEY_9},
            {GLFW.GLFW_KEY_GRAVE_ACCENT},
            {GLFW.GLFW_KEY_MINUS},
            {GLFW.GLFW_KEY_EQUAL},
            {GLFW.GLFW_KEY_LEFT_BRACKET},
            {GLFW.GLFW_KEY_RIGHT_BRACKET},
            {GLFW.GLFW_KEY_BACKSLASH},
            {GLFW.GLFW_KEY_SEMICOLON},
            {GLFW.GLFW_KEY_APOSTROPHE},
            {GLFW.GLFW_KEY_COMMA},
            {GLFW.GLFW_KEY_PERIOD},
            {GLFW.GLFW_KEY_SLASH},
            {GLFW.GLFW_KEY_WORLD_1},
            {GLFW.GLFW_KEY_WORLD_2}
    };
    private static final int[][] MAC_NATIVE_FROM_GLFW = {
            {GLFW.GLFW_KEY_A, 0x00},
            {GLFW.GLFW_KEY_S, 0x01},
            {GLFW.GLFW_KEY_D, 0x02},
            {GLFW.GLFW_KEY_F, 0x03},
            {GLFW.GLFW_KEY_H, 0x04},
            {GLFW.GLFW_KEY_G, 0x05},
            {GLFW.GLFW_KEY_Z, 0x06},
            {GLFW.GLFW_KEY_X, 0x07},
            {GLFW.GLFW_KEY_C, 0x08},
            {GLFW.GLFW_KEY_V, 0x09},
            {GLFW.GLFW_KEY_B, 0x0B},
            {GLFW.GLFW_KEY_Q, 0x0C},
            {GLFW.GLFW_KEY_W, 0x0D},
            {GLFW.GLFW_KEY_E, 0x0E},
            {GLFW.GLFW_KEY_R, 0x0F},
            {GLFW.GLFW_KEY_Y, 0x10},
            {GLFW.GLFW_KEY_T, 0x11},
            {GLFW.GLFW_KEY_1, 0x12},
            {GLFW.GLFW_KEY_2, 0x13},
            {GLFW.GLFW_KEY_3, 0x14},
            {GLFW.GLFW_KEY_4, 0x15},
            {GLFW.GLFW_KEY_6, 0x16},
            {GLFW.GLFW_KEY_5, 0x17},
            {GLFW.GLFW_KEY_EQUAL, 0x18},
            {GLFW.GLFW_KEY_9, 0x19},
            {GLFW.GLFW_KEY_7, 0x1A},
            {GLFW.GLFW_KEY_MINUS, 0x1B},
            {GLFW.GLFW_KEY_8, 0x1C},
            {GLFW.GLFW_KEY_0, 0x1D},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, 0x1E},
            {GLFW.GLFW_KEY_O, 0x1F},
            {GLFW.GLFW_KEY_U, 0x20},
            {GLFW.GLFW_KEY_LEFT_BRACKET, 0x21},
            {GLFW.GLFW_KEY_I, 0x22},
            {GLFW.GLFW_KEY_P, 0x23},
            {GLFW.GLFW_KEY_ENTER, 0x24},
            {GLFW.GLFW_KEY_L, 0x25},
            {GLFW.GLFW_KEY_J, 0x26},
            {GLFW.GLFW_KEY_APOSTROPHE, 0x27},
            {GLFW.GLFW_KEY_K, 0x28},
            {GLFW.GLFW_KEY_SEMICOLON, 0x29},
            {GLFW.GLFW_KEY_BACKSLASH, 0x2A},
            {GLFW.GLFW_KEY_COMMA, 0x2B},
            {GLFW.GLFW_KEY_SLASH, 0x2C},
            {GLFW.GLFW_KEY_N, 0x2D},
            {GLFW.GLFW_KEY_M, 0x2E},
            {GLFW.GLFW_KEY_PERIOD, 0x2F},
            {GLFW.GLFW_KEY_TAB, 0x30},
            {GLFW.GLFW_KEY_SPACE, 0x31},
            {GLFW.GLFW_KEY_GRAVE_ACCENT, 0x32},
            {GLFW.GLFW_KEY_BACKSPACE, 0x33},
            {GLFW.GLFW_KEY_ESCAPE, 0x35},
            {GLFW.GLFW_KEY_RIGHT_SUPER, 0x36},
            {GLFW.GLFW_KEY_LEFT_SUPER, 0x37},
            {GLFW.GLFW_KEY_LEFT_SHIFT, 0x38},
            {GLFW.GLFW_KEY_CAPS_LOCK, 0x39},
            {GLFW.GLFW_KEY_LEFT_ALT, 0x3A},
            {GLFW.GLFW_KEY_LEFT_CONTROL, 0x3B},
            {GLFW.GLFW_KEY_RIGHT_SHIFT, 0x3C},
            {GLFW.GLFW_KEY_RIGHT_ALT, 0x3D},
            {GLFW.GLFW_KEY_RIGHT_CONTROL, 0x3E},
            {GLFW.GLFW_KEY_F17, 0x40},
            {GLFW.GLFW_KEY_KP_DECIMAL, 0x41},
            {GLFW.GLFW_KEY_KP_MULTIPLY, 0x43},
            {GLFW.GLFW_KEY_KP_ADD, 0x45},
            {GLFW.GLFW_KEY_KP_DIVIDE, 0x4B},
            {GLFW.GLFW_KEY_KP_ENTER, 0x4C},
            {GLFW.GLFW_KEY_KP_SUBTRACT, 0x4E},
            {GLFW.GLFW_KEY_F18, 0x4F},
            {GLFW.GLFW_KEY_F19, 0x50},
            {GLFW.GLFW_KEY_KP_EQUAL, 0x51},
            {GLFW.GLFW_KEY_KP_0, 0x52},
            {GLFW.GLFW_KEY_KP_1, 0x53},
            {GLFW.GLFW_KEY_KP_2, 0x54},
            {GLFW.GLFW_KEY_KP_3, 0x55},
            {GLFW.GLFW_KEY_KP_4, 0x56},
            {GLFW.GLFW_KEY_KP_5, 0x57},
            {GLFW.GLFW_KEY_KP_6, 0x58},
            {GLFW.GLFW_KEY_KP_7, 0x59},
            {GLFW.GLFW_KEY_F20, 0x5A},
            {GLFW.GLFW_KEY_KP_8, 0x5B},
            {GLFW.GLFW_KEY_KP_9, 0x5C},
            {GLFW.GLFW_KEY_F5, 0x60},
            {GLFW.GLFW_KEY_F6, 0x61},
            {GLFW.GLFW_KEY_F7, 0x62},
            {GLFW.GLFW_KEY_F3, 0x63},
            {GLFW.GLFW_KEY_F8, 0x64},
            {GLFW.GLFW_KEY_F9, 0x65},
            {GLFW.GLFW_KEY_F11, 0x67},
            {GLFW.GLFW_KEY_F13, 0x69},
            {GLFW.GLFW_KEY_F16, 0x6A},
            {GLFW.GLFW_KEY_F14, 0x6B},
            {GLFW.GLFW_KEY_F10, 0x6D},
            {GLFW.GLFW_KEY_F12, 0x6F},
            {GLFW.GLFW_KEY_F15, 0x71},
            {GLFW.GLFW_KEY_INSERT, 0x72},
            {GLFW.GLFW_KEY_HOME, 0x73},
            {GLFW.GLFW_KEY_PAGE_UP, 0x74},
            {GLFW.GLFW_KEY_DELETE, 0x75},
            {GLFW.GLFW_KEY_F4, 0x76},
            {GLFW.GLFW_KEY_END, 0x77},
            {GLFW.GLFW_KEY_F2, 0x78},
            {GLFW.GLFW_KEY_PAGE_DOWN, 0x79},
            {GLFW.GLFW_KEY_F1, 0x7A},
            {GLFW.GLFW_KEY_LEFT, 0x7B},
            {GLFW.GLFW_KEY_RIGHT, 0x7C},
            {GLFW.GLFW_KEY_DOWN, 0x7D},
            {GLFW.GLFW_KEY_UP, 0x7E}
    };
    private static final int[][] MAC_NATIVE_FROM_CHARACTER = {
            {' ', 0x31},
            {'0', 0x1D},
            {')', 0x1D},
            {'1', 0x12},
            {'!', 0x12},
            {'2', 0x13},
            {'@', 0x13},
            {'3', 0x14},
            {'#', 0x14},
            {'4', 0x15},
            {'$', 0x15},
            {'5', 0x17},
            {'%', 0x17},
            {'6', 0x16},
            {'^', 0x16},
            {'7', 0x1A},
            {'&', 0x1A},
            {'8', 0x1C},
            {'*', 0x1C},
            {'9', 0x19},
            {'(', 0x19},
            {'a', 0x00},
            {'A', 0x00},
            {'b', 0x0B},
            {'B', 0x0B},
            {'c', 0x08},
            {'C', 0x08},
            {'d', 0x02},
            {'D', 0x02},
            {'e', 0x0E},
            {'E', 0x0E},
            {'f', 0x03},
            {'F', 0x03},
            {'g', 0x05},
            {'G', 0x05},
            {'h', 0x04},
            {'H', 0x04},
            {'i', 0x22},
            {'I', 0x22},
            {'j', 0x26},
            {'J', 0x26},
            {'k', 0x28},
            {'K', 0x28},
            {'l', 0x25},
            {'L', 0x25},
            {'m', 0x2E},
            {'M', 0x2E},
            {'n', 0x2D},
            {'N', 0x2D},
            {'o', 0x1F},
            {'O', 0x1F},
            {'p', 0x23},
            {'P', 0x23},
            {'q', 0x0C},
            {'Q', 0x0C},
            {'r', 0x0F},
            {'R', 0x0F},
            {'s', 0x01},
            {'S', 0x01},
            {'t', 0x11},
            {'T', 0x11},
            {'u', 0x20},
            {'U', 0x20},
            {'v', 0x09},
            {'V', 0x09},
            {'w', 0x0D},
            {'W', 0x0D},
            {'x', 0x07},
            {'X', 0x07},
            {'y', 0x10},
            {'Y', 0x10},
            {'z', 0x06},
            {'Z', 0x06},
            {';', 0x29},
            {':', 0x29},
            {'=', 0x18},
            {'+', 0x18},
            {',', 0x2B},
            {'<', 0x2B},
            {'-', 0x1B},
            {'_', 0x1B},
            {'.', 0x2F},
            {'>', 0x2F},
            {'/', 0x2C},
            {'?', 0x2C},
            {'`', 0x32},
            {'~', 0x32},
            {'[', 0x21},
            {'{', 0x21},
            {'\\', 0x2A},
            {'|', 0x2A},
            {']', 0x1E},
            {'}', 0x1E},
            {'\'', 0x27},
            {'"', 0x27},
            {'\b', 0x33},
            {'\t', 0x30},
            {'\r', 0x24},
            {0x1B, 0x35}
    };
    private static final Map<Integer, Integer> UNSHIFTED_CHARACTER_BY_GLFW_KEY = createByFirstColumn(GLFW_KEY_TO_CHARACTER, 1);
    private static final Map<Integer, Integer> SHIFTED_CHARACTER_BY_GLFW_KEY = createByFirstColumn(GLFW_KEY_TO_CHARACTER, 2);
    private static final Set<Integer> NUMPAD_KEYS_SET = createSetByFirstColumn(NUMPAD_KEYS);
    private static final Set<Integer> NUMPAD_TEXT_KEYS_SET = createSetByFirstColumn(NUMPAD_TEXT_KEYS);
    private static final Set<Integer> NUMPAD_TEXT_REQUIRING_NUM_LOCK_SET = createSetByFirstColumn(NUMPAD_TEXT_REQUIRING_NUM_LOCK);
    private static final Set<Integer> LAYOUT_DEPENDENT_KEYS_SET = createSetByFirstColumn(LAYOUT_DEPENDENT_KEYS);
    private static final Map<Integer, Integer> MAC_NATIVE_KEY_CODES_BY_GLFW = createByFirstColumn(MAC_NATIVE_FROM_GLFW, 1);
    private static final Map<Integer, Integer> MAC_NATIVE_KEY_CODES_BY_CHARACTER = createByFirstColumn(MAC_NATIVE_FROM_CHARACTER, 1);

    private GrapheneKeyboardMappings() {
    }

    static int windowsVkFromGlfw(int keyCode) {
        return WINDOWS_VK_BY_GLFW.getOrDefault(keyCode, 0);
    }

    static int windowsVkFromCharacter(char character) {
        return WINDOWS_VK_BY_CHARACTER.getOrDefault((int) character, 0);
    }

    static int windowsVkFromLayoutPair(int keyCode, char character) {
        return WINDOWS_VK_LAYOUT_OVERRIDES_BY_PAIR.getOrDefault(pairKey(keyCode, character), 0);
    }

    static int macNativeFromGlfw(int keyCode) {
        return MAC_NATIVE_KEY_CODES_BY_GLFW.getOrDefault(keyCode, 0);
    }

    static int macNativeFromCharacter(char character) {
        return MAC_NATIVE_KEY_CODES_BY_CHARACTER.getOrDefault((int) character, 0);
    }

    static boolean isNumpadKey(int keyCode) {
        return NUMPAD_KEYS_SET.contains(keyCode);
    }

    static boolean isNumpadTextKey(int keyCode) {
        return NUMPAD_TEXT_KEYS_SET.contains(keyCode);
    }

    static boolean requiresNumLockForText(int keyCode) {
        return NUMPAD_TEXT_REQUIRING_NUM_LOCK_SET.contains(keyCode);
    }

    static boolean isLayoutDependentKey(int keyCode) {
        return LAYOUT_DEPENDENT_KEYS_SET.contains(keyCode);
    }

    static char charFromKeyCode(int keyCode, boolean shift) {
        Map<Integer, Integer> charMap = shift ? SHIFTED_CHARACTER_BY_GLFW_KEY : UNSHIFTED_CHARACTER_BY_GLFW_KEY;
        Integer mappedCharacter = charMap.get(keyCode);
        if (mappedCharacter == null) {
            return KeyEvent.CHAR_UNDEFINED;
        }

        return (char) mappedCharacter.intValue();
    }

    private static Map<Integer, Integer> createByFirstColumn(int[][] mappingRows, int valueColumnIndex) {
        Map<Integer, Integer> mappings = new HashMap<>();
        for (int[] mappingRow : mappingRows) {
            mappings.put(mappingRow[0], mappingRow[valueColumnIndex]);
        }

        return Map.copyOf(mappings);
    }

    private static Set<Integer> createSetByFirstColumn(int[][] rows) {
        Set<Integer> values = new HashSet<>();
        for (int[] row : rows) {
            values.add(row[0]);
        }

        return Set.copyOf(values);
    }

    private static Map<Long, Integer> createByKeyAndCharacter(int[][] rows) {
        Map<Long, Integer> mappings = new HashMap<>();
        for (int[] row : rows) {
            mappings.put(pairKey(row[0], (char) row[1]), row[2]);
        }

        return Map.copyOf(mappings);
    }

    private static long pairKey(int keyCode, char character) {
        return ((long) keyCode << 32) | (character & 0xFFFFL);
    }
}
