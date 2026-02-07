package tytoo.grapheneui.input;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

public final class GrapheneKeyCodeUtil {
    private static final String SHIFTED_DIGITS = ")!@#$%^&*(";
    private static final int AWT_VK_RETURN = 0x0D;

    private GrapheneKeyCodeUtil() {
    }

    public static int toAwtKeyCode(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case GLFW.GLFW_KEY_TAB -> KeyEvent.VK_TAB;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> AWT_VK_RETURN;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> KeyEvent.VK_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> KeyEvent.VK_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> KeyEvent.VK_ALT;
            case GLFW.GLFW_KEY_ESCAPE -> KeyEvent.VK_ESCAPE;
            case GLFW.GLFW_KEY_SPACE -> KeyEvent.VK_SPACE;
            case GLFW.GLFW_KEY_LEFT -> KeyEvent.VK_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> KeyEvent.VK_RIGHT;
            case GLFW.GLFW_KEY_UP -> KeyEvent.VK_UP;
            case GLFW.GLFW_KEY_DOWN -> KeyEvent.VK_DOWN;
            case GLFW.GLFW_KEY_DELETE -> KeyEvent.VK_DELETE;
            case GLFW.GLFW_KEY_HOME -> KeyEvent.VK_HOME;
            case GLFW.GLFW_KEY_END -> KeyEvent.VK_END;
            case GLFW.GLFW_KEY_PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case GLFW.GLFW_KEY_PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            default -> keyCode;
        };
    }

    public static char toCharacter(int keyCode, boolean shift) {
        if (isAlphabetKey(keyCode)) {
            return toAlphabetCharacter(keyCode, shift);
        }

        if (isDigitKey(keyCode)) {
            return toDigitCharacter(keyCode, shift);
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

    private static char toSpecialCharacter(int keyCode, boolean shift) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_MINUS -> shift ? '_' : '-';
            case GLFW.GLFW_KEY_EQUAL -> shift ? '+' : '=';
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
