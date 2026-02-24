package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class GrapheneLinuxKeyEventPlatformResolver implements GrapheneKeyEventPlatformResolver {
    private static final int XK_BACK_SPACE = 0xFF08;
    private static final int XK_TAB = 0xFF09;
    private static final int XK_RETURN = 0xFF0D;
    private static final int XK_ESCAPE = 0xFF1B;
    private static final int XK_LEFT = 0xFF51;
    private static final int XK_UP = 0xFF52;
    private static final int XK_RIGHT = 0xFF53;
    private static final int XK_DOWN = 0xFF54;
    private static final int XK_DELETE = 0xFFFF;

    private static final int[][] LINUX_NATIVE_KEY_CODES = {
            {GLFW.GLFW_KEY_BACKSPACE, XK_BACK_SPACE},
            {GLFW.GLFW_KEY_DELETE, XK_DELETE},
            {GLFW.GLFW_KEY_DOWN, XK_DOWN},
            {GLFW.GLFW_KEY_ENTER, XK_RETURN},
            {GLFW.GLFW_KEY_KP_ENTER, XK_RETURN},
            {GLFW.GLFW_KEY_ESCAPE, XK_ESCAPE},
            {GLFW.GLFW_KEY_LEFT, XK_LEFT},
            {GLFW.GLFW_KEY_RIGHT, XK_RIGHT},
            {GLFW.GLFW_KEY_TAB, XK_TAB},
            {GLFW.GLFW_KEY_UP, XK_UP}
    };
    private static final Map<Integer, Integer> LINUX_NATIVE_KEY_CODES_BY_KEY = createByFirstColumn(LINUX_NATIVE_KEY_CODES);
    private static final int[][] CONTROL_SHIFTED_KEY_CODES = {
            {GLFW.GLFW_KEY_2, 0},
            {GLFW.GLFW_KEY_6, 0x1E},
            {GLFW.GLFW_KEY_MINUS, 0x1F}
    };
    private static final int[][] CONTROL_UNSHIFTED_KEY_CODES = {
            {GLFW.GLFW_KEY_LEFT_BRACKET, 0x1B},
            {GLFW.GLFW_KEY_BACKSLASH, 0x1C},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, 0x1D},
            {GLFW.GLFW_KEY_ENTER, 0x0A},
            {GLFW.GLFW_KEY_KP_ENTER, 0x0A}
    };
    private static final int[][] ENTER_KEYS = {
            {GLFW.GLFW_KEY_ENTER},
            {GLFW.GLFW_KEY_KP_ENTER}
    };
    private static final Map<Integer, Integer> CONTROL_SHIFTED_BY_KEY = createByFirstColumn(CONTROL_SHIFTED_KEY_CODES);
    private static final Map<Integer, Integer> CONTROL_UNSHIFTED_BY_KEY = createByFirstColumn(CONTROL_UNSHIFTED_KEY_CODES);
    private static final Set<Integer> ENTER_KEYS_SET = createSetByFirstColumn(ENTER_KEYS);

    private static boolean isPrintableCharacter(char character) {
        return character >= 0x20 && !Character.isISOControl(character);
    }

    private static boolean hasModifier(int modifiers, int modifier) {
        return (modifiers & modifier) != 0;
    }

    private static int resolveLinuxNativeKeyCode(int keyCode, char character) {
        Integer nativeKeyCode = LINUX_NATIVE_KEY_CODES_BY_KEY.get(keyCode);
        if (nativeKeyCode != null) {
            return nativeKeyCode;
        }

        return isPrintableCharacter(character) ? character : 0;
    }

    private static char resolveLinuxUnmodifiedCharacter(int keyCode, char character) {
        if (ENTER_KEYS_SET.contains(keyCode)) {
            return '\r';
        }

        int nativeKeyCode = resolveLinuxNativeKeyCode(keyCode, character);
        if (nativeKeyCode != 0) {
            return (char) nativeKeyCode;
        }

        return character;
    }

    private static char getControlCharacter(int keyCode, boolean shift) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return (char) (keyCode - GLFW.GLFW_KEY_A + 1);
        }

        Map<Integer, Integer> controlMap = shift ? CONTROL_SHIFTED_BY_KEY : CONTROL_UNSHIFTED_BY_KEY;
        Integer controlCharacter = controlMap.get(keyCode);
        if (controlCharacter == null) {
            return 0;
        }

        return (char) controlCharacter.intValue();
    }

    private static Map<Integer, Integer> createByFirstColumn(int[][] rows) {
        Map<Integer, Integer> mappings = new HashMap<>();
        for (int[] row : rows) {
            mappings.put(row[0], row[1]);
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

    @Override
    public int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        int nativeKeyCode = resolveLinuxNativeKeyCode(keyCode, character);
        if (nativeKeyCode != 0) {
            return nativeKeyCode;
        }

        return GrapheneKeyEventPlatformResolver.super.getNativeKeyCode(keyCode, scanCode, character, pressed);
    }


    @Override
    public boolean isSystemKey(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    @Override
    public char getRawEventUnmodifiedCharacter(int keyCode, char character, int modifiers) {
        return resolveLinuxUnmodifiedCharacter(keyCode, character);
    }

    @Override
    public char getRawEventCharacter(int keyCode, char unmodifiedCharacter, int modifiers) {
        if (hasModifier(modifiers, GLFW.GLFW_MOD_CONTROL)) {
            return getControlCharacter(keyCode, hasModifier(modifiers, GLFW.GLFW_MOD_SHIFT));
        }

        return unmodifiedCharacter;
    }
}
