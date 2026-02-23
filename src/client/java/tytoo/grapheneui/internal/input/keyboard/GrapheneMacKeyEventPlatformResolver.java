package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

final class GrapheneMacKeyEventPlatformResolver extends GrapheneBaseKeyEventPlatformResolver {
    private static final char MAC_UP_ARROW = '\uF700';
    private static final char MAC_DOWN_ARROW = '\uF701';
    private static final char MAC_LEFT_ARROW = '\uF702';
    private static final char MAC_RIGHT_ARROW = '\uF703';
    private static final char MAC_INSERT = '\uF727';
    private static final char MAC_FORWARD_DELETE = '\uF728';
    private static final char MAC_HOME = '\uF729';
    private static final char MAC_END = '\uF72B';
    private static final char MAC_PAGE_UP = '\uF72C';
    private static final char MAC_PAGE_DOWN = '\uF72D';
    private static final char MAC_FUNCTION_KEY_START = '\uF700';
    private static final char MAC_FUNCTION_KEY_END = '\uF8FF';

    @Override
    public char resolveRawKeyCharacter(int keyCode, int scanCode, int modifiers) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> 0x7F;
            case GLFW.GLFW_KEY_LEFT -> MAC_LEFT_ARROW;
            case GLFW.GLFW_KEY_RIGHT -> MAC_RIGHT_ARROW;
            case GLFW.GLFW_KEY_UP -> MAC_UP_ARROW;
            case GLFW.GLFW_KEY_DOWN -> MAC_DOWN_ARROW;
            case GLFW.GLFW_KEY_INSERT -> MAC_INSERT;
            case GLFW.GLFW_KEY_DELETE -> MAC_FORWARD_DELETE;
            case GLFW.GLFW_KEY_HOME -> MAC_HOME;
            case GLFW.GLFW_KEY_END -> MAC_END;
            case GLFW.GLFW_KEY_PAGE_UP -> MAC_PAGE_UP;
            case GLFW.GLFW_KEY_PAGE_DOWN -> MAC_PAGE_DOWN;
            default -> super.resolveRawKeyCharacter(keyCode, scanCode, modifiers);
        };
    }

    @Override
    public char toRawEventCharacter(char character) {
        return character;
    }

    @Override
    public int resolveNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        int mappedKeyCode = GrapheneMacKeyCodeMapping.resolveFromGlfwKey(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        int charMappedKeyCode = GrapheneMacKeyCodeMapping.resolveFromCharacter(normalizeTypedCharacter(character));
        if (charMappedKeyCode != 0) {
            return charMappedKeyCode;
        }

        return Math.max(scanCode, 0);
    }

    @Override
    public int resolveCharNativeKeyCode(char character) {
        return GrapheneMacKeyCodeMapping.resolveFromCharacter(normalizeTypedCharacter(character));
    }

    @Override
    public char normalizeTypedCharacter(char character) {
        if (character == 0x7F) {
            return '\b';
        }

        if (character == '\n') {
            return '\r';
        }

        if (isUnsupportedTypedCharacter(character)) {
            return KeyEvent.CHAR_UNDEFINED;
        }

        return character;
    }

    private static boolean isUnsupportedTypedCharacter(char character) {
        if (character >= MAC_FUNCTION_KEY_START && character <= MAC_FUNCTION_KEY_END) {
            return true;
        }

        return Character.isISOControl(character)
                && character != '\b'
                && character != '\t'
                && character != '\r';
    }
}
