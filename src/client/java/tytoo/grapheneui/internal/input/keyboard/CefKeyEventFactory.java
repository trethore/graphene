package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneCefModifierUtil;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

import java.awt.event.KeyEvent;

final class CefKeyEventFactory {
    private final GrapheneKeyEventPlatformResolver platformStrategy;

    CefKeyEventFactory(GrapheneKeyEventPlatformResolver platformStrategy) {
        this.platformStrategy = platformStrategy;
    }

    private static int toCefModifiers(int modifiers, int keyCode, boolean numLockEnabled) {
        int cefModifiers = GrapheneCefModifierUtil.toCefCommonModifiers(modifiers);

        if ((modifiers & GLFW.GLFW_MOD_CAPS_LOCK) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_CAPS_LOCK_ON;
        }

        if (numLockEnabled) {
            cefModifiers |= EventFlags.EVENTFLAG_NUM_LOCK_ON;
        }

        if (GrapheneKeyCodeUtil.isNumpadKey(keyCode)) {
            cefModifiers |= EventFlags.EVENTFLAG_IS_KEY_PAD;
        }

        if (isLeftModifierKey(keyCode)) {
            cefModifiers |= EventFlags.EVENTFLAG_IS_LEFT;
        } else if (isRightModifierKey(keyCode)) {
            cefModifiers |= EventFlags.EVENTFLAG_IS_RIGHT;
        }

        return cefModifiers;
    }

    private static boolean isLeftModifierKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT,
                 GLFW.GLFW_KEY_LEFT_CONTROL,
                 GLFW.GLFW_KEY_LEFT_ALT,
                 GLFW.GLFW_KEY_LEFT_SUPER -> true;
            default -> false;
        };
    }

    private static boolean isRightModifierKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT,
                 GLFW.GLFW_KEY_RIGHT_CONTROL,
                 GLFW.GLFW_KEY_RIGHT_ALT,
                 GLFW.GLFW_KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }

    private static char normalizeCharacter(char character) {
        return character == KeyEvent.CHAR_UNDEFINED ? 0 : character;
    }

    CefKeyEvent createRawKeyEvent(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean pressed,
            char character,
            boolean numLockEnabled
    ) {
        char normalizedCharacter = normalizeCharacter(character);
        int resolvedScanCode = platformStrategy.resolveScanCode(keyCode, scanCode);
        char rawEventUnmodifiedCharacter = platformStrategy.getRawEventUnmodifiedCharacter(
                keyCode,
                normalizedCharacter,
                modifiers
        );
        char rawEventCharacter = platformStrategy.getRawEventCharacter(keyCode, rawEventUnmodifiedCharacter, modifiers);
        return new CefKeyEvent(
                platformStrategy.getRawEventType(pressed, keyCode, rawEventCharacter),
                toCefModifiers(modifiers, keyCode, numLockEnabled),
                GrapheneDomKeyCodeMapper.resolveDomKeyCode(keyCode, normalizedCharacter, numLockEnabled),
                platformStrategy.getNativeKeyCode(keyCode, resolvedScanCode, normalizedCharacter, pressed),
                platformStrategy.isSystemKey(modifiers),
                rawEventCharacter,
                rawEventUnmodifiedCharacter,
                platformStrategy.getScanCode(resolvedScanCode)
        );
    }

    CefKeyEvent createCharEvent(int keyCode, char character, int modifiers, boolean numLockEnabled) {
        char normalizedCharacter = normalizeCharacter(character);
        return new CefKeyEvent(
                CefKeyEvent.KEYEVENT_CHAR,
                toCefModifiers(modifiers, keyCode, numLockEnabled),
                normalizedCharacter,
                platformStrategy.getCharNativeKeyCode(normalizedCharacter),
                platformStrategy.isSystemKey(modifiers),
                normalizedCharacter,
                normalizedCharacter
        );
    }
}
