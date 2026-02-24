package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public final class GrapheneKeyboardInputBridge {
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;
    private static final char CEF_CHAR_UNDEFINED = 0;

    private final GrapheneInputLockState lockState = new GrapheneInputLockState();
    private final GrapheneKeyEventPlatformResolver platformStrategy = GrapheneKeyEventPlatformResolver.create();

    private char pendingSyntheticCharacter = CEF_CHAR_UNDEFINED;
    private long pendingSyntheticCharacterTimestamp;
    private boolean rightAltPressed;

    private static char normalizeCefCharacter(char character) {
        return character == KeyEvent.CHAR_UNDEFINED ? CEF_CHAR_UNDEFINED : character;
    }

    public void keyTyped(Consumer<CefKeyEvent> keyEventSink, char character, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();

        char normalizedCharacter = GrapheneKeyboardSharedUtil.normalizeTypedCharacter(character);

        if (isDuplicateSyntheticTypedCharacter(normalizedCharacter)) {
            return;
        }

        if (normalizedCharacter == KeyEvent.CHAR_UNDEFINED || normalizedCharacter == CEF_CHAR_UNDEFINED) {
            return;
        }

        int charEventModifiers = platformStrategy.sanitizeCharEventModifiers(modifiers, rightAltPressed);
        boolean numLockEnabled = lockState.isNumLockEnabled(modifiers);

        CefKeyEvent cefEvent = createCharEvent(
                GLFW.GLFW_KEY_UNKNOWN,
                normalizedCharacter,
                charEventModifiers,
                numLockEnabled
        );
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

        lockState.updateCachedNumLockState(keyCode, pressed);

        char layoutCharacter = GrapheneKeyboardSharedUtil.resolveLayoutCharacter(keyCode, scanCode, modifiers);
        char character = platformStrategy.resolveRawKeyCharacter(keyCode, layoutCharacter);

        boolean numLockEnabled = lockState.isNumLockEnabled(modifiers);
        boolean treatNumpadAsText = GrapheneKeyboardMappings.isNumpadTextKey(keyCode)
                && isNumpadTextModeEnabled(keyCode, numLockEnabled);

        if (treatNumpadAsText) {
            dispatchNumpadTextKeyEvent(keyEventSink, keyCode, pressed, modifiers, scanCode, numLockEnabled);
            if (pressed) {
                dispatchSyntheticTypedCharacter(keyEventSink, keyCode, character, modifiers, numLockEnabled);
            }
            return;
        }

        CefKeyEvent cefEvent = createRawKeyEvent(
                keyCode,
                scanCode,
                modifiers,
                pressed,
                character,
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
        CefKeyEvent cefEvent = createRawKeyEvent(
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
        char normalizedCharacter = GrapheneKeyboardSharedUtil.normalizeTypedCharacter(character);
        if (normalizedCharacter == KeyEvent.CHAR_UNDEFINED || normalizedCharacter == CEF_CHAR_UNDEFINED) {
            return;
        }

        rememberSyntheticTypedCharacter(normalizedCharacter);
        int charEventModifiers = platformStrategy.sanitizeCharEventModifiers(modifiers, rightAltPressed);
        CefKeyEvent cefEvent = createCharEvent(keyCode, normalizedCharacter, charEventModifiers, numLockEnabled);
        keyEventSink.accept(cefEvent);
    }

    private CefKeyEvent createRawKeyEvent(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean pressed,
            char character,
            boolean numLockEnabled
    ) {
        char normalizedCharacter = normalizeCefCharacter(character);
        int resolvedScanCode = platformStrategy.resolveScanCode(keyCode, scanCode);
        char rawEventUnmodifiedCharacter = platformStrategy.getRawEventUnmodifiedCharacter(
                keyCode,
                normalizedCharacter,
                modifiers
        );
        char rawEventCharacter = platformStrategy.getRawEventCharacter(keyCode, rawEventUnmodifiedCharacter, modifiers);
        return new CefKeyEvent(
                platformStrategy.getRawEventType(pressed, keyCode, rawEventCharacter),
                GrapheneKeyboardSharedUtil.toCefKeyboardModifiers(modifiers, keyCode, numLockEnabled),
                GrapheneKeyboardSharedUtil.resolveDomKeyCode(keyCode, normalizedCharacter, numLockEnabled),
                platformStrategy.getNativeKeyCode(keyCode, resolvedScanCode, normalizedCharacter, pressed),
                platformStrategy.isSystemKey(modifiers),
                rawEventCharacter,
                rawEventUnmodifiedCharacter,
                platformStrategy.getScanCode(resolvedScanCode)
        );
    }

    private CefKeyEvent createCharEvent(int keyCode, char character, int modifiers, boolean numLockEnabled) {
        char normalizedCharacter = normalizeCefCharacter(character);
        return new CefKeyEvent(
                CefKeyEvent.KEYEVENT_CHAR,
                GrapheneKeyboardSharedUtil.toCefKeyboardModifiers(modifiers, keyCode, numLockEnabled),
                normalizedCharacter,
                platformStrategy.getCharNativeKeyCode(normalizedCharacter),
                platformStrategy.isSystemKey(modifiers),
                normalizedCharacter,
                normalizedCharacter
        );
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

    private boolean isNumpadTextModeEnabled(int keyCode, boolean numLockEnabled) {
        return !GrapheneKeyboardMappings.requiresNumLockForText(keyCode) || numLockEnabled;
    }
}
