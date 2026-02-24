package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneKeyCodeUtil;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public final class GrapheneKeyboardInputBridge {
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;
    private static final char CEF_CHAR_UNDEFINED = 0;

    private final GrapheneInputLockState lockState = new GrapheneInputLockState();
    private final GrapheneKeyEventPlatformResolver platformStrategy = GrapheneKeyEventPlatformResolver.create();
    private final CefKeyEventFactory eventFactory = new CefKeyEventFactory(platformStrategy);

    private char pendingSyntheticCharacter = CEF_CHAR_UNDEFINED;
    private long pendingSyntheticCharacterTimestamp;
    private boolean rightAltPressed;

    public void keyTyped(Consumer<CefKeyEvent> keyEventSink, char character, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();

        char normalizedCharacter = GrapheneCharacterMapper.normalizeTypedCharacter(character);

        if (isDuplicateSyntheticTypedCharacter(normalizedCharacter)) {
            return;
        }

        if (normalizedCharacter == KeyEvent.CHAR_UNDEFINED || normalizedCharacter == CEF_CHAR_UNDEFINED) {
            return;
        }

        int charEventModifiers = platformStrategy.sanitizeCharEventModifiers(modifiers, rightAltPressed);
        boolean numLockEnabled = lockState.isNumLockEnabled(modifiers);

        CefKeyEvent cefEvent = eventFactory.createCharEvent(
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

        lockState.updateFallbackNumLockState(keyCode, pressed);

        char layoutCharacter = GrapheneCharacterMapper.resolveLayoutCharacter(keyCode, scanCode, modifiers);
        char character = platformStrategy.resolveRawKeyCharacter(keyCode, layoutCharacter);

        boolean numLockEnabled = lockState.isNumLockEnabled(modifiers);
        boolean treatNumpadAsText = GrapheneKeyCodeUtil.isNumpadTextKey(keyCode)
                && isNumpadTextModeEnabled(keyCode, numLockEnabled);

        if (treatNumpadAsText) {
            dispatchNumpadTextKeyEvent(keyEventSink, keyCode, pressed, modifiers, scanCode, numLockEnabled);
            if (pressed) {
                dispatchSyntheticTypedCharacter(keyEventSink, keyCode, character, modifiers, numLockEnabled);
            }
            return;
        }

        CefKeyEvent cefEvent = eventFactory.createRawKeyEvent(
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
        CefKeyEvent cefEvent = eventFactory.createRawKeyEvent(
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
        char normalizedCharacter = GrapheneCharacterMapper.normalizeTypedCharacter(character);
        if (normalizedCharacter == KeyEvent.CHAR_UNDEFINED || normalizedCharacter == CEF_CHAR_UNDEFINED) {
            return;
        }

        rememberSyntheticTypedCharacter(normalizedCharacter);
        int charEventModifiers = platformStrategy.sanitizeCharEventModifiers(modifiers, rightAltPressed);
        CefKeyEvent cefEvent = eventFactory.createCharEvent(keyCode, normalizedCharacter, charEventModifiers, numLockEnabled);
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

    private boolean isNumpadTextModeEnabled(int keyCode, boolean numLockEnabled) {
        return !GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) || numLockEnabled;
    }
}
