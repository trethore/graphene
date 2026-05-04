package tytoo.grapheneui.internal.input.keyboard.platform;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.keyboard.GrapheneKeyboardSharedUtil;
import tytoo.grapheneui.internal.input.keyboard.mapping.GrapheneKeyboardMappings;

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

    private static final Map<Integer, Integer> RAW_KEY_CHARACTER_OVERRIDES_BY_KEY = Map.ofEntries(
            Map.entry(GLFW.GLFW_KEY_BACKSPACE, 0x7F),
            Map.entry(GLFW.GLFW_KEY_LEFT, (int) MAC_LEFT_ARROW),
            Map.entry(GLFW.GLFW_KEY_RIGHT, (int) MAC_RIGHT_ARROW),
            Map.entry(GLFW.GLFW_KEY_UP, (int) MAC_UP_ARROW),
            Map.entry(GLFW.GLFW_KEY_DOWN, (int) MAC_DOWN_ARROW),
            Map.entry(GLFW.GLFW_KEY_INSERT, (int) MAC_INSERT),
            Map.entry(GLFW.GLFW_KEY_DELETE, (int) MAC_FORWARD_DELETE),
            Map.entry(GLFW.GLFW_KEY_HOME, (int) MAC_HOME),
            Map.entry(GLFW.GLFW_KEY_END, (int) MAC_END),
            Map.entry(GLFW.GLFW_KEY_PAGE_UP, (int) MAC_PAGE_UP),
            Map.entry(GLFW.GLFW_KEY_PAGE_DOWN, (int) MAC_PAGE_DOWN)
    );

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
