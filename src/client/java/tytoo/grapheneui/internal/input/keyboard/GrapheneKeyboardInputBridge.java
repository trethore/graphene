package tytoo.grapheneui.internal.input.keyboard;

import org.cef.input.CefKeyEvent;
import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneInputModifierUtil;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public final class GrapheneKeyboardInputBridge {
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;
    private static final char CEF_CHAR_UNDEFINED = 0;
    private static final String INFO_LOG_PREFIX = "[Graphene][INFO][KeyboardInput] ";

    private final GrapheneInputLockState lockState = new GrapheneInputLockState();
    private final GrapheneKeyEventPlatformResolver platformStrategy = GrapheneKeyEventPlatformResolver.create();

    private char pendingSyntheticCharacter = CEF_CHAR_UNDEFINED;
    private long pendingSyntheticCharacterTimestamp;
    private boolean rightAltPressed;

    private static char normalizeCefCharacter(char character) {
        return character == KeyEvent.CHAR_UNDEFINED ? CEF_CHAR_UNDEFINED : character;
    }

    private static void logInfo(String message) {
        System.out.println(INFO_LOG_PREFIX + message);
    }

    private static String describeCharacter(char character) {
        if (character == KeyEvent.CHAR_UNDEFINED || character == CEF_CHAR_UNDEFINED) {
            return "undefined";
        }

        String hexCode = "0x" + Integer.toHexString(character).toUpperCase();
        if (Character.isISOControl(character)) {
            return "control(" + hexCode + ")";
        }

        return "'" + character + "'(" + hexCode + ")";
    }

    private static String describeGlfwModifiers(int modifiers) {
        StringBuilder builder = new StringBuilder();
        builder.append(modifiers).append("[");
        appendFlag(builder, modifiers, GLFW.GLFW_MOD_SHIFT, "SHIFT");
        appendFlag(builder, modifiers, GLFW.GLFW_MOD_CONTROL, "CTRL");
        appendFlag(builder, modifiers, GLFW.GLFW_MOD_ALT, "ALT");
        appendFlag(builder, modifiers, GLFW.GLFW_MOD_SUPER, "SUPER");
        appendFlag(builder, modifiers, GLFW.GLFW_MOD_CAPS_LOCK, "CAPS");
        appendFlag(builder, modifiers, GLFW.GLFW_MOD_NUM_LOCK, "NUM");
        builder.append("]");
        return builder.toString();
    }

    private static String describeCefModifiers(int modifiers) {
        StringBuilder builder = new StringBuilder();
        builder.append(modifiers).append("[");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_SHIFT_DOWN, "SHIFT");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_CONTROL_DOWN, "CTRL");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_ALT_DOWN, "ALT");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_COMMAND_DOWN, "COMMAND");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_CAPS_LOCK_ON, "CAPS");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_NUM_LOCK_ON, "NUM");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_IS_KEY_PAD, "KEYPAD");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_IS_LEFT, "LEFT");
        appendFlag(builder, modifiers, EventFlags.EVENTFLAG_IS_RIGHT, "RIGHT");
        builder.append("]");
        return builder.toString();
    }

    private static void appendFlag(StringBuilder builder, int value, int mask, String label) {
        if ((value & mask) == 0) {
            return;
        }

        if (builder.charAt(builder.length() - 1) != '[') {
            builder.append(',');
        }

        builder.append(label);
    }

    private static String keyEventTypeName(int eventType) {
        return switch (eventType) {
            case CefKeyEvent.KEYEVENT_RAWKEYDOWN -> "RAWKEYDOWN";
            case CefKeyEvent.KEYEVENT_KEYDOWN -> "KEYDOWN";
            case CefKeyEvent.KEYEVENT_KEYUP -> "KEYUP";
            case CefKeyEvent.KEYEVENT_CHAR -> "CHAR";
            default -> "UNKNOWN(" + eventType + ")";
        };
    }

    private static void logDispatchedCefEvent(
            String source,
            int keyCode,
            int scanCode,
            boolean pressed,
            CefKeyEvent event
    ) {
        logInfo(
                source
                        + " keyCode=" + keyCode
                        + " scanCode=" + scanCode
                        + " pressed=" + pressed
                        + " type=" + keyEventTypeName(event.type)
                        + " windowsKeyCode=" + event.windows_key_code
                        + " nativeKeyCode=" + event.native_key_code
                        + " modifiers=" + describeCefModifiers(event.modifiers)
                        + " character=" + describeCharacter(event.character)
                        + " unmodifiedCharacter=" + describeCharacter(event.unmodified_character)
        );
    }

    public void keyTyped(Consumer<CefKeyEvent> keyEventSink, char character, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();
        int resolvedModifiers = GrapheneInputModifierUtil.mergeWithCurrentModifiers(modifiers);
        logInfo(
                "keyTyped input character=" + describeCharacter(character)
                        + " modifiers=" + describeGlfwModifiers(modifiers)
                        + " resolvedModifiers=" + describeGlfwModifiers(resolvedModifiers)
        );

        char normalizedCharacter = GrapheneKeyboardSharedUtil.normalizeTypedCharacter(character);

        if (isDuplicateSyntheticTypedCharacter(normalizedCharacter)) {
            logInfo("keyTyped ignored duplicate synthetic character=" + describeCharacter(normalizedCharacter));
            return;
        }

        if (normalizedCharacter == KeyEvent.CHAR_UNDEFINED || normalizedCharacter == CEF_CHAR_UNDEFINED) {
            logInfo("keyTyped ignored unsupported character=" + describeCharacter(normalizedCharacter));
            return;
        }

        int charEventModifiers = platformStrategy.sanitizeCharEventModifiers(resolvedModifiers, rightAltPressed);
        boolean numLockEnabled = lockState.isNumLockEnabled(resolvedModifiers);

        CefKeyEvent cefEvent = createCharEvent(
                GLFW.GLFW_KEY_UNKNOWN,
                normalizedCharacter,
                charEventModifiers,
                numLockEnabled
        );
        logDispatchedCefEvent("keyTyped dispatch", GLFW.GLFW_KEY_UNKNOWN, 0, true, cefEvent);
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
        int resolvedModifiers = GrapheneInputModifierUtil.mergeWithCurrentModifiers(modifiers);
        logInfo(
                "keyEvent input keyCode=" + keyCode
                        + " scanCode=" + scanCode
                        + " pressed=" + pressed
                        + " modifiers=" + describeGlfwModifiers(modifiers)
                        + " resolvedModifiers=" + describeGlfwModifiers(resolvedModifiers)
        );
        if (keyCode == GLFW.GLFW_KEY_RIGHT_ALT) {
            rightAltPressed = pressed;
        }

        lockState.updateCachedNumLockState(keyCode, pressed);

        char layoutCharacter = GrapheneKeyboardSharedUtil.resolveLayoutCharacter(keyCode, scanCode, resolvedModifiers);
        char character = platformStrategy.resolveRawKeyCharacter(keyCode, layoutCharacter);

        boolean numLockEnabled = lockState.isNumLockEnabled(resolvedModifiers);
        boolean treatNumpadAsText = GrapheneKeyboardMappings.isNumpadTextKey(keyCode)
                && isNumpadTextModeEnabled(keyCode, numLockEnabled);

        if (treatNumpadAsText) {
            dispatchNumpadTextKeyEvent(keyEventSink, keyCode, pressed, resolvedModifiers, scanCode, numLockEnabled);
            if (pressed) {
                dispatchSyntheticTypedCharacter(keyEventSink, keyCode, character, resolvedModifiers, numLockEnabled);
            }
            return;
        }

        CefKeyEvent cefEvent = createRawKeyEvent(
                keyCode,
                scanCode,
                resolvedModifiers,
                pressed,
                character,
                numLockEnabled
        );
        logDispatchedCefEvent("keyEvent dispatch", keyCode, scanCode, pressed, cefEvent);
        keyEventSink.accept(cefEvent);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            dispatchSyntheticTypedCharacter(keyEventSink, keyCode, '\b', resolvedModifiers, numLockEnabled);
        }

        if (pressed && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            dispatchSyntheticTypedCharacter(keyEventSink, keyCode, '\r', resolvedModifiers, numLockEnabled);
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
        logDispatchedCefEvent("numpad dispatch", glfwKeyCode, scanCode, pressed, cefEvent);
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
        logDispatchedCefEvent("synthetic char dispatch", keyCode, 0, true, cefEvent);
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
