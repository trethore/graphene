package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

final class GrapheneMacKeyEventPlatformResolver implements GrapheneKeyEventPlatformResolver {
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

    private static final int[][] RAW_KEY_CHARACTER_OVERRIDES = {
            {GLFW.GLFW_KEY_BACKSPACE, 0x7F},
            {GLFW.GLFW_KEY_LEFT, MAC_LEFT_ARROW},
            {GLFW.GLFW_KEY_RIGHT, MAC_RIGHT_ARROW},
            {GLFW.GLFW_KEY_UP, MAC_UP_ARROW},
            {GLFW.GLFW_KEY_DOWN, MAC_DOWN_ARROW},
            {GLFW.GLFW_KEY_INSERT, MAC_INSERT},
            {GLFW.GLFW_KEY_DELETE, MAC_FORWARD_DELETE},
            {GLFW.GLFW_KEY_HOME, MAC_HOME},
            {GLFW.GLFW_KEY_END, MAC_END},
            {GLFW.GLFW_KEY_PAGE_UP, MAC_PAGE_UP},
            {GLFW.GLFW_KEY_PAGE_DOWN, MAC_PAGE_DOWN}
    };
    private static final Map<Integer, Integer> RAW_KEY_CHARACTER_OVERRIDES_BY_KEY = createByFirstColumn(RAW_KEY_CHARACTER_OVERRIDES);
    private static final int[][] CONTROL_CHARACTER_OVERRIDES = {
            {GLFW.GLFW_KEY_LEFT_BRACKET, 27},
            {GLFW.GLFW_KEY_BACKSLASH, 28},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, 29}
    };
    private static final Map<Integer, Integer> CONTROL_CHARACTER_OVERRIDES_BY_KEY = createByFirstColumn(CONTROL_CHARACTER_OVERRIDES);

    private static boolean hasModifier(int modifiers, int modifier) {
        return (modifiers & modifier) != 0;
    }

    private static char getControlCharacter(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return (char) (keyCode - GLFW.GLFW_KEY_A + 1);
        }

        Integer mappedCharacter = CONTROL_CHARACTER_OVERRIDES_BY_KEY.get(keyCode);
        if (mappedCharacter == null) {
            return 0;
        }

        return (char) mappedCharacter.intValue();
    }

    private static Map<Integer, Integer> createByFirstColumn(int[][] rows) {
        Map<Integer, Integer> mappings = new HashMap<>();
        for (int[] row : rows) {
            mappings.put(row[0], row[1]);
        }

        return Map.copyOf(mappings);
    }

    @Override
    public char resolveRawKeyCharacter(int keyCode, char layoutCharacter) {
        Integer mappedCharacter = RAW_KEY_CHARACTER_OVERRIDES_BY_KEY.get(keyCode);
        if (mappedCharacter == null) {
            return layoutCharacter;
        }

        return (char) mappedCharacter.intValue();
    }

    @Override
    public char getRawEventUnmodifiedCharacter(int keyCode, char character, int modifiers) {
        return character;
    }

    @Override
    public char getRawEventCharacter(int keyCode, char unmodifiedCharacter, int modifiers) {
        if (hasModifier(modifiers, GLFW.GLFW_MOD_CONTROL)) {
            char controlCharacter = getControlCharacter(keyCode);
            if (controlCharacter != 0) {
                return controlCharacter;
            }
        }

        return unmodifiedCharacter;
    }

    @Override
    public int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (GrapheneKeyboardMappings.hasMacNativeFromGlfw(keyCode)) {
            return GrapheneKeyboardMappings.macNativeFromGlfw(keyCode);
        }

        char normalizedCharacter = GrapheneKeyboardSharedUtil.normalizeTypedCharacter(character);
        if (GrapheneKeyboardMappings.hasMacNativeFromCharacter(normalizedCharacter)) {
            return GrapheneKeyboardMappings.macNativeFromCharacter(normalizedCharacter);
        }

        return Math.max(scanCode, 0);
    }

    @Override
    public int getCharNativeKeyCode(char character) {
        char normalizedCharacter = GrapheneKeyboardSharedUtil.normalizeTypedCharacter(character);
        if (!GrapheneKeyboardMappings.hasMacNativeFromCharacter(normalizedCharacter)) {
            return 0;
        }

        return GrapheneKeyboardMappings.macNativeFromCharacter(normalizedCharacter);
    }

}
