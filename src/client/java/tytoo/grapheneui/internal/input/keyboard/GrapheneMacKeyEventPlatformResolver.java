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

    private static Map<Integer, Integer> createByFirstColumn(int[][] rows) {
        Map<Integer, Integer> mappings = new HashMap<>();
        for (int[] row : rows) {
            mappings.put(row[0], row[1]);
        }

        return Map.copyOf(mappings);
    }

    @Override
    public boolean isSystemKey(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
    }

    @Override
    public int getNativeVirtualKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        Integer rawCharacterOverride = RAW_KEY_CHARACTER_OVERRIDES_BY_KEY.get(keyCode);
        if (rawCharacterOverride != null) {
            return rawCharacterOverride;
        }

        if (GrapheneKeyboardMappings.hasMacNativeFromGlfw(keyCode)) {
            return GrapheneKeyboardMappings.macNativeFromGlfw(keyCode);
        }

        char normalizedCharacter = GrapheneKeyboardSharedUtil.normalizeTypedCharacter(character);
        if (GrapheneKeyboardMappings.hasMacNativeFromCharacter(normalizedCharacter)) {
            return GrapheneKeyboardMappings.macNativeFromCharacter(normalizedCharacter);
        }

        return Math.max(scanCode, 0);
    }
}
