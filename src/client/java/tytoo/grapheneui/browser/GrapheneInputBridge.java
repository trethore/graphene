package tytoo.grapheneui.browser;

import org.cef.input.CefKeyEvent;
import org.cef.input.CefMouseEvent;
import org.cef.input.CefMouseWheelEvent;
import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.input.GrapheneKeyCodeUtil;

import java.awt.*;
import java.awt.event.KeyEvent;

final class GrapheneInputBridge {
    private static final int MOUSE_LEFT_BUTTON = 0;
    private static final int MOUSE_RIGHT_BUTTON = 1;
    private static final int MOUSE_MIDDLE_BUTTON = 2;
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;
    private static final char CEF_CHAR_UNDEFINED = 0;

    private final Component uiComponent = new Component() {
    };
    private final GrapheneInputLockState lockState = new GrapheneInputLockState();
    private final GrapheneKeyEventPlatformResolver keyEventPlatformResolver = GrapheneKeyEventPlatformResolver.create();
    private char pendingSyntheticCharacter = CEF_CHAR_UNDEFINED;
    private long pendingSyntheticCharacterTimestamp;
    private boolean rightAltPressed;

    private static int remapMouseCode(int button) {
        return switch (button) {
            case MOUSE_LEFT_BUTTON -> CefMouseEvent.BUTTON_LEFT;
            case MOUSE_RIGHT_BUTTON -> CefMouseEvent.BUTTON_RIGHT;
            case MOUSE_MIDDLE_BUTTON -> CefMouseEvent.BUTTON_MIDDLE;
            default -> CefMouseEvent.BUTTON_NONE;
        };
    }

    private static int toCefMouseModifiers(int modifiers) {
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

        return cefModifiers;
    }

    private static int toCefButtonDownModifier(int button) {
        return switch (remapMouseCode(button)) {
            case CefMouseEvent.BUTTON_LEFT -> EventFlags.EVENTFLAG_LEFT_MOUSE_BUTTON;
            case CefMouseEvent.BUTTON_MIDDLE -> EventFlags.EVENTFLAG_MIDDLE_MOUSE_BUTTON;
            case CefMouseEvent.BUTTON_RIGHT -> EventFlags.EVENTFLAG_RIGHT_MOUSE_BUTTON;
            default -> EventFlags.EVENTFLAG_NONE;
        };
    }

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

    Component uiComponent() {
        return uiComponent;
    }

    void mouseMoved(GrapheneBrowser browser, int x, int y, int modifiers) {
        int cefModifiers = toCefMouseModifiers(modifiers);
        CefMouseEvent event = new CefMouseEvent(
                CefMouseEvent.MOUSEEVENT_MOVED,
                x,
                y,
                cefModifiers,
                CefMouseEvent.BUTTON_NONE,
                1
        );
        browser.dispatchMouseEvent(event);
    }

    void mouseDragged(GrapheneBrowser browser, double x, double y, int button) {
        int cefModifiers = toCefButtonDownModifier(button);
        CefMouseEvent event = new CefMouseEvent(
                CefMouseEvent.MOUSEEVENT_DRAGGED,
                (int) x,
                (int) y,
                cefModifiers,
                CefMouseEvent.BUTTON_NONE,
                1
        );
        browser.dispatchMouseEvent(event);
    }

    void mouseInteracted(GrapheneBrowser browser, int x, int y, int modifiers, int button, boolean pressed, int clickCount) {
        int cefButton = remapMouseCode(button);
        int cefModifiers = toCefMouseModifiers(modifiers);
        if (pressed) {
            cefModifiers |= toCefButtonDownModifier(button);
        }

        CefMouseEvent event = new CefMouseEvent(
                pressed ? CefMouseEvent.MOUSEEVENT_PRESSED : CefMouseEvent.MOUSEEVENT_RELEASED,
                x,
                y,
                cefModifiers,
                cefButton,
                clickCount
        );
        browser.dispatchMouseEvent(event);
    }

    void mouseScrolled(GrapheneBrowser browser, int x, int y, int modifiers, int amount, int rotation) {
        int cefModifiers = toCefMouseModifiers(modifiers);
        int delta = amount * rotation;
        int deltaX;
        int deltaY;
        if ((cefModifiers & EventFlags.EVENTFLAG_SHIFT_DOWN) != 0) {
            deltaX = delta;
            deltaY = 0;
        } else {
            deltaX = 0;
            deltaY = delta;
        }

        CefMouseWheelEvent event = new CefMouseWheelEvent(
                x,
                y,
                cefModifiers,
                deltaX,
                deltaY
        );
        browser.dispatchMouseWheelEvent(event);
    }

    void keyTyped(GrapheneBrowser browser, char character, int modifiers) {
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
        browser.dispatchCefKeyEvent(cefEvent);
    }

    void keyEventByKeyCode(GrapheneBrowser browser, int keyCode, int scanCode, int modifiers, boolean pressed) {
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
            dispatchNumpadTextKeyEvent(browser, keyCode, pressed, modifiers, scanCode, numLockEnabled);
            if (pressed) {
                dispatchSyntheticTypedCharacter(browser, keyCode, character, modifiers, numLockEnabled);
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
        browser.dispatchCefKeyEvent(cefEvent);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            dispatchSyntheticTypedCharacter(browser, keyCode, '\b', modifiers, numLockEnabled);
        }

        if (pressed && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            dispatchSyntheticTypedCharacter(browser, keyCode, '\r', modifiers, numLockEnabled);
        }
    }

    private void dispatchNumpadTextKeyEvent(
            GrapheneBrowser browser,
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
        browser.dispatchCefKeyEvent(cefEvent);
    }

    private void dispatchSyntheticTypedCharacter(
            GrapheneBrowser browser,
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
        browser.dispatchCefKeyEvent(cefEvent);
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
