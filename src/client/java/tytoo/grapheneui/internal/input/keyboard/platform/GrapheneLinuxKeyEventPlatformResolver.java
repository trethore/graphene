package tytoo.grapheneui.internal.input.keyboard.platform;

import org.lwjgl.glfw.GLFW;

import java.util.Map;

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

    private static final Map<Integer, Integer> LINUX_NATIVE_KEY_CODES_BY_KEY = Map.ofEntries(
            Map.entry(GLFW.GLFW_KEY_BACKSPACE, XK_BACK_SPACE),
            Map.entry(GLFW.GLFW_KEY_DELETE, XK_DELETE),
            Map.entry(GLFW.GLFW_KEY_DOWN, XK_DOWN),
            Map.entry(GLFW.GLFW_KEY_ENTER, XK_RETURN),
            Map.entry(GLFW.GLFW_KEY_KP_ENTER, XK_RETURN),
            Map.entry(GLFW.GLFW_KEY_ESCAPE, XK_ESCAPE),
            Map.entry(GLFW.GLFW_KEY_LEFT, XK_LEFT),
            Map.entry(GLFW.GLFW_KEY_RIGHT, XK_RIGHT),
            Map.entry(GLFW.GLFW_KEY_TAB, XK_TAB),
            Map.entry(GLFW.GLFW_KEY_UP, XK_UP)
    );

    private static boolean isPrintableCharacter(char character) {
        return character >= 0x20 && !Character.isISOControl(character);
    }

    private static int resolveLinuxNativeKeyCode(int keyCode, char character) {
        Integer nativeKeyCode = LINUX_NATIVE_KEY_CODES_BY_KEY.get(keyCode);
        if (nativeKeyCode != null) {
            return nativeKeyCode;
        }

        return isPrintableCharacter(character) ? character : 0;
    }

    @Override
    public int getNativeVirtualKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        int nativeKeyCode = resolveLinuxNativeKeyCode(keyCode, character);
        if (nativeKeyCode != 0) {
            return nativeKeyCode;
        }

        return GrapheneKeyEventPlatformResolver.super.getNativeVirtualKeyCode(keyCode, scanCode, character, pressed);
    }

    @Override
    public boolean isSystemKey(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }
}
