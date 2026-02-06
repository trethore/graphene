package tytoo.grapheneui.client.input;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

public final class GrapheneKeyCodeUtil {
    private GrapheneKeyCodeUtil() {
    }

    public static int toAwtKeyCode(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case GLFW.GLFW_KEY_TAB -> KeyEvent.VK_TAB;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> KeyEvent.VK_ENTER;
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
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            char character = (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
            return shift ? Character.toUpperCase(character) : character;
        }

        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            char character = (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
            if (shift) {
                return switch (character) {
                    case '1' -> '!';
                    case '2' -> '@';
                    case '3' -> '#';
                    case '4' -> '$';
                    case '5' -> '%';
                    case '6' -> '^';
                    case '7' -> '&';
                    case '8' -> '*';
                    case '9' -> '(';
                    case '0' -> ')';
                    default -> character;
                };
            }

            return character;
        }

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
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> '\n';
            case GLFW.GLFW_KEY_BACKSPACE -> '\b';
            case GLFW.GLFW_KEY_SPACE -> ' ';
            default -> KeyEvent.CHAR_UNDEFINED;
        };
    }
}
