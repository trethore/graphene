package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public final class GrapheneKeyboardInputBridge {
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;
    private static final char CEF_CHAR_UNDEFINED = 0;

    private final GrapheneInputLockState lockState = new GrapheneInputLockState();
    private final GrapheneKeyEventPlatformResolver keyEventPlatformResolver = GrapheneKeyEventPlatformResolver.create();
    private char pendingSyntheticCharacter = CEF_CHAR_UNDEFINED;
    private long pendingSyntheticCharacterTimestamp;
    private boolean rightAltPressed;

    private static int toCefModifiers(int modifiers, int keyCode, boolean numLockEnabled) {
        int cefModifiers = EventFlags.EVENTFLAG_NONE;
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_SHIFT_DOWN;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_CONTROL_DOWN;
        }

        if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_ALT_DOWN;
        }

        if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0) {
            cefModifiers |= EventFlags.EVENTFLAG_COMMAND_DOWN;
        }

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
        return character == KeyEvent.CHAR_UNDEFINED ? CEF_CHAR_UNDEFINED : character;
    }

    private static boolean isNumpadTextModeEnabled(int keyCode, boolean numLockEnabled) {
        return !GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) || numLockEnabled;
    }

    private CefKeyEvent createCefRawKeyEvent(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean pressed,
            char character,
            boolean numLockEnabled
    ) {
        char normalizedCharacter = normalizeCharacter(character);
        return new CefKeyEvent(
                keyEventPlatformResolver.resolveRawKeyEventType(pressed, keyCode, normalizedCharacter),
                toCefModifiers(modifiers, keyCode, numLockEnabled),
                keyEventPlatformResolver.resolveWindowsKeyCode(keyCode, scanCode, normalizedCharacter, numLockEnabled),
                keyEventPlatformResolver.resolveNativeKeyCode(keyCode, scanCode, normalizedCharacter, pressed),
                keyEventPlatformResolver.isSystemKey(modifiers),
                normalizedCharacter,
                normalizedCharacter,
                keyEventPlatformResolver.resolveScanCode(scanCode)
        );
    }

    private CefKeyEvent createCefCharEvent(int keyCode, char character, int modifiers, boolean numLockEnabled) {
        char normalizedCharacter = normalizeCharacter(character);
        return new CefKeyEvent(
                CefKeyEvent.KEYEVENT_CHAR,
                toCefModifiers(modifiers, keyCode, numLockEnabled),
                normalizedCharacter,
                keyEventPlatformResolver.resolveCharNativeKeyCode(normalizedCharacter),
                keyEventPlatformResolver.isSystemKey(modifiers),
                normalizedCharacter,
                normalizedCharacter
        );
    }

    public void keyTyped(Consumer<CefKeyEvent> keyEventSink, char character, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();

        if (isDuplicateSyntheticTypedCharacter(character)) {
            return;
        }

        if (character == KeyEvent.CHAR_UNDEFINED) {
            return;
        }

        int charEventModifiers = keyEventPlatformResolver.sanitizeCharEventModifiers(modifiers, rightAltPressed);
        boolean numLockEnabled = lockState.isNumLockEnabled(modifiers);
        CefKeyEvent cefEvent = createCefCharEvent(GLFW.GLFW_KEY_UNKNOWN, character, charEventModifiers, numLockEnabled);
        keyEventSink.accept(cefEvent);
    }

    public void keyEventByKeyCode(
            Consumer<CefKeyEvent> keyEventSink,
            int keyCode,
            int scanCode,
            int modifiers,
            boolean pressed
    ) {
        lockState.ensureLockKeyModifiersEnabled();
        if (keyCode == GLFW.GLFW_KEY_RIGHT_ALT) {
            rightAltPressed = pressed;
        }

        lockState.updateFallbackNumLockState(keyCode, pressed);

        char character = keyEventPlatformResolver.resolveRawKeyCharacter(keyCode, scanCode, modifiers);
        boolean numLockEnabled = lockState.isNumLockEnabled(modifiers);
        boolean treatNumpadAsText = GrapheneKeyCodeUtil.isNumpadTextKey(keyCode)
                && shouldTreatNumpadAsText(keyCode, numLockEnabled);

        if (treatNumpadAsText) {
            dispatchNumpadTextKeyEvent(keyEventSink, keyCode, pressed, modifiers, scanCode, numLockEnabled);
            if (pressed) {
                dispatchSyntheticTypedCharacter(keyEventSink, keyCode, character, modifiers, numLockEnabled);
            }

            return;
        }

        char rawEventCharacter = keyEventPlatformResolver.toRawEventCharacter(character);
        CefKeyEvent cefEvent = createCefRawKeyEvent(
                keyCode,
                scanCode,
                modifiers,
                pressed,
                rawEventCharacter,
                numLockEnabled
        );
        keyEventSink.accept(cefEvent);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            dispatchSyntheticTypedCharacter(keyEventSink, keyCode, '\b', modifiers, numLockEnabled);
        }

        if (pressed && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            dispatchSyntheticTypedCharacter(keyEventSink, keyCode, '\r', modifiers, numLockEnabled);
        }
    }

    private void dispatchNumpadTextKeyEvent(
            Consumer<CefKeyEvent> keyEventSink,
            int glfwKeyCode,
            boolean pressed,
            int modifiers,
            int scanCode,
            boolean numLockEnabled
    ) {
        CefKeyEvent cefEvent = createCefRawKeyEvent(
                glfwKeyCode,
                scanCode,
                modifiers,
                pressed,
                KeyEvent.CHAR_UNDEFINED,
                numLockEnabled
        );
        keyEventSink.accept(cefEvent);
    }

    private void dispatchSyntheticTypedCharacter(
            Consumer<CefKeyEvent> keyEventSink,
            int keyCode,
            char character,
            int modifiers,
            boolean numLockEnabled
    ) {
        if (character == KeyEvent.CHAR_UNDEFINED || character == CEF_CHAR_UNDEFINED) {
            return;
        }

        rememberSyntheticTypedCharacter(character);
        int charEventModifiers = keyEventPlatformResolver.sanitizeCharEventModifiers(modifiers, rightAltPressed);
        CefKeyEvent cefEvent = createCefCharEvent(keyCode, character, charEventModifiers, numLockEnabled);
        keyEventSink.accept(cefEvent);
    }

    private void rememberSyntheticTypedCharacter(char character) {
        pendingSyntheticCharacter = character;
        pendingSyntheticCharacterTimestamp = System.currentTimeMillis();
    }

    private boolean isDuplicateSyntheticTypedCharacter(char character) {
        if (pendingSyntheticCharacter == CEF_CHAR_UNDEFINED) {
            return false;
        }

        long now = System.currentTimeMillis();
        boolean duplicate = now - pendingSyntheticCharacterTimestamp <= SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS
                && pendingSyntheticCharacter == character;
        pendingSyntheticCharacter = CEF_CHAR_UNDEFINED;
        pendingSyntheticCharacterTimestamp = 0L;
        return duplicate;
    }

    private boolean shouldTreatNumpadAsText(int keyCode, boolean numLockEnabled) {
        return isNumpadTextModeEnabled(keyCode, numLockEnabled);
    }
}
