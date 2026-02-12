package tytoo.grapheneui.internal.input;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

public final class GrapheneKeyCodeUtil {
    private static final String SHIFTED_DIGITS = ")!@#$%^&*(";
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

    private GrapheneKeyCodeUtil() {
    }

    @SuppressWarnings("java:S1479") // Sonar complains about this being too long.
    public static int toWindowsKeyCode(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case GLFW.GLFW_KEY_TAB -> KeyEvent.VK_TAB;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> AWT_VK_RETURN;
            case GLFW.GLFW_KEY_0 -> KeyEvent.VK_0;
            case GLFW.GLFW_KEY_1 -> KeyEvent.VK_1;
            case GLFW.GLFW_KEY_2 -> KeyEvent.VK_2;
            case GLFW.GLFW_KEY_3 -> KeyEvent.VK_3;
            case GLFW.GLFW_KEY_4 -> KeyEvent.VK_4;
            case GLFW.GLFW_KEY_5 -> KeyEvent.VK_5;
            case GLFW.GLFW_KEY_6 -> KeyEvent.VK_6;
            case GLFW.GLFW_KEY_7 -> KeyEvent.VK_7;
            case GLFW.GLFW_KEY_8 -> KeyEvent.VK_8;
            case GLFW.GLFW_KEY_9 -> KeyEvent.VK_9;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> KeyEvent.VK_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> KeyEvent.VK_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> KeyEvent.VK_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> KeyEvent.VK_WINDOWS;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> WINDOWS_VK_OEM_3;
            case GLFW.GLFW_KEY_MINUS -> WINDOWS_VK_OEM_MINUS;
            case GLFW.GLFW_KEY_EQUAL -> WINDOWS_VK_OEM_PLUS;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> WINDOWS_VK_OEM_4;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> WINDOWS_VK_OEM_6;
            case GLFW.GLFW_KEY_BACKSLASH -> WINDOWS_VK_OEM_5;
            case GLFW.GLFW_KEY_SEMICOLON -> WINDOWS_VK_OEM_1;
            case GLFW.GLFW_KEY_APOSTROPHE -> WINDOWS_VK_OEM_7;
            case GLFW.GLFW_KEY_COMMA -> WINDOWS_VK_OEM_COMMA;
            case GLFW.GLFW_KEY_PERIOD -> WINDOWS_VK_OEM_PERIOD;
            case GLFW.GLFW_KEY_SLASH -> WINDOWS_VK_OEM_2;
            case GLFW.GLFW_KEY_WORLD_1, GLFW.GLFW_KEY_WORLD_2 -> WINDOWS_VK_OEM_102;
            case GLFW.GLFW_KEY_ESCAPE -> KeyEvent.VK_ESCAPE;
            case GLFW.GLFW_KEY_SPACE -> KeyEvent.VK_SPACE;
            case GLFW.GLFW_KEY_INSERT -> KeyEvent.VK_INSERT;
            case GLFW.GLFW_KEY_LEFT -> KeyEvent.VK_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> KeyEvent.VK_RIGHT;
            case GLFW.GLFW_KEY_UP -> KeyEvent.VK_UP;
            case GLFW.GLFW_KEY_DOWN -> KeyEvent.VK_DOWN;
            case GLFW.GLFW_KEY_CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK;
            case GLFW.GLFW_KEY_SCROLL_LOCK -> KeyEvent.VK_SCROLL_LOCK;
            case GLFW.GLFW_KEY_NUM_LOCK -> KeyEvent.VK_NUM_LOCK;
            case GLFW.GLFW_KEY_PRINT_SCREEN -> KeyEvent.VK_PRINTSCREEN;
            case GLFW.GLFW_KEY_PAUSE -> KeyEvent.VK_PAUSE;
            case GLFW.GLFW_KEY_DELETE -> KeyEvent.VK_DELETE;
            case GLFW.GLFW_KEY_HOME -> KeyEvent.VK_HOME;
            case GLFW.GLFW_KEY_END -> KeyEvent.VK_END;
            case GLFW.GLFW_KEY_PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case GLFW.GLFW_KEY_PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            case GLFW.GLFW_KEY_F1 -> KeyEvent.VK_F1;
            case GLFW.GLFW_KEY_F2 -> KeyEvent.VK_F2;
            case GLFW.GLFW_KEY_F3 -> KeyEvent.VK_F3;
            case GLFW.GLFW_KEY_F4 -> KeyEvent.VK_F4;
            case GLFW.GLFW_KEY_F5 -> KeyEvent.VK_F5;
            case GLFW.GLFW_KEY_F6 -> KeyEvent.VK_F6;
            case GLFW.GLFW_KEY_F7 -> KeyEvent.VK_F7;
            case GLFW.GLFW_KEY_F8 -> KeyEvent.VK_F8;
            case GLFW.GLFW_KEY_F9 -> KeyEvent.VK_F9;
            case GLFW.GLFW_KEY_F10 -> KeyEvent.VK_F10;
            case GLFW.GLFW_KEY_F11 -> KeyEvent.VK_F11;
            case GLFW.GLFW_KEY_F12 -> KeyEvent.VK_F12;
            case GLFW.GLFW_KEY_KP_0 -> KeyEvent.VK_NUMPAD0;
            case GLFW.GLFW_KEY_KP_1 -> KeyEvent.VK_NUMPAD1;
            case GLFW.GLFW_KEY_KP_2 -> KeyEvent.VK_NUMPAD2;
            case GLFW.GLFW_KEY_KP_3 -> KeyEvent.VK_NUMPAD3;
            case GLFW.GLFW_KEY_KP_4 -> KeyEvent.VK_NUMPAD4;
            case GLFW.GLFW_KEY_KP_5 -> KeyEvent.VK_NUMPAD5;
            case GLFW.GLFW_KEY_KP_6 -> KeyEvent.VK_NUMPAD6;
            case GLFW.GLFW_KEY_KP_7 -> KeyEvent.VK_NUMPAD7;
            case GLFW.GLFW_KEY_KP_8 -> KeyEvent.VK_NUMPAD8;
            case GLFW.GLFW_KEY_KP_9 -> KeyEvent.VK_NUMPAD9;
            case GLFW.GLFW_KEY_KP_DECIMAL -> KeyEvent.VK_DECIMAL;
            case GLFW.GLFW_KEY_KP_DIVIDE -> KeyEvent.VK_DIVIDE;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> KeyEvent.VK_MULTIPLY;
            case GLFW.GLFW_KEY_KP_SUBTRACT -> KeyEvent.VK_SUBTRACT;
            case GLFW.GLFW_KEY_KP_ADD -> KeyEvent.VK_ADD;
            case GLFW.GLFW_KEY_KP_EQUAL -> KeyEvent.VK_EQUALS;
            default -> 0;
        };
    }

    public static int toAwtKeyCode(int keyCode) {
        return toWindowsKeyCode(keyCode);
    }

    public static int toWindowsKeyCodeFromLinuxCharacter(char character, int keyCode) {
        return switch (character) {
            case ',' -> WINDOWS_VK_OEM_COMMA;
            case ';' -> WINDOWS_VK_OEM_PERIOD;
            case ':' -> WINDOWS_VK_OEM_2;
            case '.' -> WINDOWS_VK_OEM_PERIOD;
            case '/', '?' -> WINDOWS_VK_OEM_2;
            case '<' -> WINDOWS_VK_OEM_COMMA;
            case '>' -> WINDOWS_VK_OEM_PERIOD;
            case '*' -> WINDOWS_VK_OEM_5;
            case '%' -> WINDOWS_VK_OEM_3;
            case '$' -> WINDOWS_VK_OEM_1;
            case '!' -> WINDOWS_VK_OEM_8;
            case '&' -> KeyEvent.VK_1;
            case 'e', 'E', 'é', 'É' -> keyCode == GLFW.GLFW_KEY_2 ? KeyEvent.VK_2 : 0;
            case '"' -> KeyEvent.VK_3;
            case '\'' -> KeyEvent.VK_4;
            case '(' -> KeyEvent.VK_5;
            case '-' -> keyCode == GLFW.GLFW_KEY_6 ? KeyEvent.VK_6 : WINDOWS_VK_OEM_MINUS;
            case 'è', 'È' -> KeyEvent.VK_7;
            case '_' -> KeyEvent.VK_8;
            case 'ç', 'Ç' -> KeyEvent.VK_9;
            case 'à', 'À' -> KeyEvent.VK_0;
            case ')' -> WINDOWS_VK_OEM_MINUS;
            case '=' -> WINDOWS_VK_OEM_PLUS;
            default -> 0;
        };
    }

    public static boolean isNumpadKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_KP_0,
                 GLFW.GLFW_KEY_KP_1,
                 GLFW.GLFW_KEY_KP_2,
                 GLFW.GLFW_KEY_KP_3,
                 GLFW.GLFW_KEY_KP_4,
                 GLFW.GLFW_KEY_KP_5,
                 GLFW.GLFW_KEY_KP_6,
                 GLFW.GLFW_KEY_KP_7,
                 GLFW.GLFW_KEY_KP_8,
                 GLFW.GLFW_KEY_KP_9,
                 GLFW.GLFW_KEY_KP_DECIMAL,
                 GLFW.GLFW_KEY_KP_DIVIDE,
                 GLFW.GLFW_KEY_KP_MULTIPLY,
                 GLFW.GLFW_KEY_KP_SUBTRACT,
                 GLFW.GLFW_KEY_KP_ADD,
                 GLFW.GLFW_KEY_KP_ENTER,
                 GLFW.GLFW_KEY_KP_EQUAL -> true;
            default -> false;
        };
    }

    public static boolean isNumpadTextKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_KP_0,
                 GLFW.GLFW_KEY_KP_1,
                 GLFW.GLFW_KEY_KP_2,
                 GLFW.GLFW_KEY_KP_3,
                 GLFW.GLFW_KEY_KP_4,
                 GLFW.GLFW_KEY_KP_5,
                 GLFW.GLFW_KEY_KP_6,
                 GLFW.GLFW_KEY_KP_7,
                 GLFW.GLFW_KEY_KP_8,
                 GLFW.GLFW_KEY_KP_9,
                 GLFW.GLFW_KEY_KP_DECIMAL,
                 GLFW.GLFW_KEY_KP_DIVIDE,
                 GLFW.GLFW_KEY_KP_MULTIPLY,
                 GLFW.GLFW_KEY_KP_SUBTRACT,
                 GLFW.GLFW_KEY_KP_ADD,
                 GLFW.GLFW_KEY_KP_EQUAL -> true;
            default -> false;
        };
    }

    public static boolean isNumpadOperatorKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_KP_DIVIDE,
                 GLFW.GLFW_KEY_KP_MULTIPLY,
                 GLFW.GLFW_KEY_KP_SUBTRACT,
                 GLFW.GLFW_KEY_KP_ADD -> true;
            default -> false;
        };
    }

    public static boolean requiresNumLockForText(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_KP_0,
                 GLFW.GLFW_KEY_KP_1,
                 GLFW.GLFW_KEY_KP_2,
                 GLFW.GLFW_KEY_KP_3,
                 GLFW.GLFW_KEY_KP_4,
                 GLFW.GLFW_KEY_KP_5,
                 GLFW.GLFW_KEY_KP_6,
                 GLFW.GLFW_KEY_KP_7,
                 GLFW.GLFW_KEY_KP_8,
                 GLFW.GLFW_KEY_KP_9,
                 GLFW.GLFW_KEY_KP_DECIMAL -> true;
            default -> false;
        };
    }

    public static char toCharacter(int keyCode, boolean shift) {
        if (isAlphabetKey(keyCode)) {
            return toAlphabetCharacter(keyCode, shift);
        }

        if (isDigitKey(keyCode)) {
            return toDigitCharacter(keyCode, shift);
        }

        if (isNumpadDigitKey(keyCode)) {
            return toNumpadDigitCharacter(keyCode);
        }

        return toSpecialCharacter(keyCode, shift);
    }

    private static boolean isAlphabetKey(int keyCode) {
        return keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z;
    }

    private static boolean isDigitKey(int keyCode) {
        return keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9;
    }

    private static char toAlphabetCharacter(int keyCode, boolean shift) {
        char character = (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
        return shift ? Character.toUpperCase(character) : character;
    }

    private static char toDigitCharacter(int keyCode, boolean shift) {
        int digitIndex = keyCode - GLFW.GLFW_KEY_0;
        if (shift) {
            return SHIFTED_DIGITS.charAt(digitIndex);
        }

        return (char) ('0' + digitIndex);
    }

    private static boolean isNumpadDigitKey(int keyCode) {
        return keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9;
    }

    private static char toNumpadDigitCharacter(int keyCode) {
        return (char) ('0' + (keyCode - GLFW.GLFW_KEY_KP_0));
    }

    private static char toSpecialCharacter(int keyCode, boolean shift) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_MINUS -> shift ? '_' : '-';
            case GLFW.GLFW_KEY_EQUAL -> shift ? '+' : '=';
            case GLFW.GLFW_KEY_KP_DECIMAL -> '.';
            case GLFW.GLFW_KEY_KP_DIVIDE -> '/';
            case GLFW.GLFW_KEY_KP_MULTIPLY -> '*';
            case GLFW.GLFW_KEY_KP_SUBTRACT -> '-';
            case GLFW.GLFW_KEY_KP_ADD -> '+';
            case GLFW.GLFW_KEY_KP_EQUAL -> '=';
            case GLFW.GLFW_KEY_BACKSLASH -> shift ? '|' : '\\';
            case GLFW.GLFW_KEY_SLASH -> shift ? '?' : '/';
            case GLFW.GLFW_KEY_SEMICOLON -> shift ? ':' : ';';
            case GLFW.GLFW_KEY_COMMA -> shift ? '<' : ',';
            case GLFW.GLFW_KEY_PERIOD -> shift ? '>' : '.';
            case GLFW.GLFW_KEY_APOSTROPHE -> shift ? '"' : '\'';
            case GLFW.GLFW_KEY_LEFT_BRACKET -> shift ? '{' : '[';
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> shift ? '}' : ']';
            case GLFW.GLFW_KEY_TAB -> '\t';
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> '\r';
            case GLFW.GLFW_KEY_BACKSPACE -> '\b';
            case GLFW.GLFW_KEY_SPACE -> ' ';
            default -> KeyEvent.CHAR_UNDEFINED;
        };
    }
}
