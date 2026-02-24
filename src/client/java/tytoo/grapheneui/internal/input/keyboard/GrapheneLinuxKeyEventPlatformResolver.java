package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;

final class GrapheneLinuxKeyEventPlatformResolver extends GrapheneBaseKeyEventPlatformResolver {
    private static boolean isPrintableCharacter(char character) {
        return character >= 0x20 && !Character.isISOControl(character);
    }

    @Override
    public int getNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (isPrintableCharacter(character)) {
            return character;
        }

        return super.getNativeKeyCode(keyCode, scanCode, character, pressed);
    }

    @Override
    public int getRawEventType(boolean pressed, int keyCode, char character) {
        if (!pressed) {
            return CefKeyEvent.KEYEVENT_KEYUP;
        }

        if (GrapheneDomKeyCodeMapper.isLayoutDependentKey(keyCode) && isPrintableCharacter(character)) {
            return CefKeyEvent.KEYEVENT_KEYDOWN;
        }

        return CefKeyEvent.KEYEVENT_RAWKEYDOWN;
    }

    @Override
    public boolean isSystemKey(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    @Override
    public char toRawEventCharacter(char character) {
        return character;
    }
}
