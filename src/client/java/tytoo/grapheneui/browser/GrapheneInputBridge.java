package tytoo.grapheneui.browser;

import org.cef.input.CefKeyEvent;
import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.input.GrapheneKeyCodeUtil;
import tytoo.grapheneui.mc.McClient;
import tytoo.grapheneui.platform.GraphenePlatform;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Optional;

final class GrapheneInputBridge {
    private static final int MOUSE_LEFT_BUTTON = 0;
    private static final int MOUSE_RIGHT_BUTTON = 1;
    private static final int MOUSE_MIDDLE_BUTTON = 2;
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;
    private static final char CEF_CHAR_UNDEFINED = 0;

    private final Component uiComponent = new Component() {
    };
    private char pendingSyntheticCharacter = CEF_CHAR_UNDEFINED;
    private long pendingSyntheticCharacterTimestamp;
    private boolean lockKeyModifiersEnabled;
    private boolean rightAltPressed;
    private boolean fallbackNumLockState;
    private boolean fallbackNumLockStateKnown;

    GrapheneInputBridge() {
        ensureLockKeyModifiersEnabled();
        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            fallbackNumLockState = toolkitNumLockState.get();
            fallbackNumLockStateKnown = true;
        }
    }

    private static int remapMouseCode(int button) {
        return switch (button) {
            case MOUSE_LEFT_BUTTON -> MouseEvent.BUTTON1;
            case MOUSE_RIGHT_BUTTON -> MouseEvent.BUTTON3;
            case MOUSE_MIDDLE_BUTTON -> MouseEvent.BUTTON2;
            default -> MouseEvent.NOBUTTON;
        };
    }

    private static int toAwtModifiers(int modifiers) {
        int awtModifiers = 0;
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            awtModifiers |= InputEvent.SHIFT_DOWN_MASK;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            awtModifiers |= InputEvent.CTRL_DOWN_MASK;
        }

        if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            awtModifiers |= InputEvent.ALT_DOWN_MASK;
        }

        if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0) {
            awtModifiers |= InputEvent.META_DOWN_MASK;
        }

        return awtModifiers;
    }

    private static int toAwtButtonDownModifier(int button) {
        return switch (remapMouseCode(button)) {
            case MouseEvent.BUTTON1 -> InputEvent.BUTTON1_DOWN_MASK;
            case MouseEvent.BUTTON2 -> InputEvent.BUTTON2_DOWN_MASK;
            case MouseEvent.BUTTON3 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> 0;
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

    private static boolean isSystemKey(int modifiers) {
        if (GraphenePlatform.isLinux()) {
            return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        }

        return false;
    }

    private static int resolveWindowsKeyCode(int keyCode, int scanCode, char character, boolean numLockEnabled) {
        int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
        if (!GraphenePlatform.isWindows() || scanCode <= 0) {
            if (mappedKeyCode != 0) {
                return mappedKeyCode;
            }

            return character;
        }

        if (GrapheneKeyCodeUtil.isNumpadKey(keyCode)) {
            if (GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) && !numLockEnabled) {
                return 0;
            }

            return mappedKeyCode;
        }

        if (isLayoutDependentKey(keyCode)) {
            return 0;
        }

        return mappedKeyCode;
    }

    private static int resolveNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (GraphenePlatform.isWindows()) {
            if (scanCode <= 0) {
                return 0;
            }

            return CefKeyEvent.buildWindowsNativeKeyCode(scanCode, isWindowsExtendedKey(keyCode), !pressed);
        }

        if (scanCode > 0) {
            return scanCode;
        }

        int mappedKeyCode = GrapheneKeyCodeUtil.toWindowsKeyCode(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        return character;
    }

    private static long resolveScanCode(int scanCode) {
        if (scanCode <= 0) {
            return 0L;
        }

        if (GraphenePlatform.isWindows()) {
            return scanCode & 0xFFL;
        }

        return scanCode;
    }

    private static boolean isLayoutDependentKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_A,
                 GLFW.GLFW_KEY_B,
                 GLFW.GLFW_KEY_C,
                 GLFW.GLFW_KEY_D,
                 GLFW.GLFW_KEY_E,
                 GLFW.GLFW_KEY_F,
                 GLFW.GLFW_KEY_G,
                 GLFW.GLFW_KEY_H,
                 GLFW.GLFW_KEY_I,
                 GLFW.GLFW_KEY_J,
                 GLFW.GLFW_KEY_K,
                 GLFW.GLFW_KEY_L,
                 GLFW.GLFW_KEY_M,
                 GLFW.GLFW_KEY_N,
                 GLFW.GLFW_KEY_O,
                 GLFW.GLFW_KEY_P,
                 GLFW.GLFW_KEY_Q,
                 GLFW.GLFW_KEY_R,
                 GLFW.GLFW_KEY_S,
                 GLFW.GLFW_KEY_T,
                 GLFW.GLFW_KEY_U,
                 GLFW.GLFW_KEY_V,
                 GLFW.GLFW_KEY_W,
                 GLFW.GLFW_KEY_X,
                 GLFW.GLFW_KEY_Y,
                 GLFW.GLFW_KEY_Z,
                 GLFW.GLFW_KEY_0,
                 GLFW.GLFW_KEY_1,
                 GLFW.GLFW_KEY_2,
                 GLFW.GLFW_KEY_3,
                 GLFW.GLFW_KEY_4,
                 GLFW.GLFW_KEY_5,
                 GLFW.GLFW_KEY_6,
                 GLFW.GLFW_KEY_7,
                 GLFW.GLFW_KEY_8,
                 GLFW.GLFW_KEY_9,
                 GLFW.GLFW_KEY_GRAVE_ACCENT,
                 GLFW.GLFW_KEY_MINUS,
                 GLFW.GLFW_KEY_EQUAL,
                 GLFW.GLFW_KEY_LEFT_BRACKET,
                 GLFW.GLFW_KEY_RIGHT_BRACKET,
                 GLFW.GLFW_KEY_BACKSLASH,
                 GLFW.GLFW_KEY_SEMICOLON,
                 GLFW.GLFW_KEY_APOSTROPHE,
                 GLFW.GLFW_KEY_COMMA,
                 GLFW.GLFW_KEY_PERIOD,
                 GLFW.GLFW_KEY_SLASH,
                 GLFW.GLFW_KEY_WORLD_1,
                 GLFW.GLFW_KEY_WORLD_2 -> true;
            default -> false;
        };
    }

    private static boolean isWindowsExtendedKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_ALT,
                 GLFW.GLFW_KEY_RIGHT_CONTROL,
                 GLFW.GLFW_KEY_INSERT,
                 GLFW.GLFW_KEY_DELETE,
                 GLFW.GLFW_KEY_HOME,
                 GLFW.GLFW_KEY_END,
                 GLFW.GLFW_KEY_PAGE_UP,
                 GLFW.GLFW_KEY_PAGE_DOWN,
                 GLFW.GLFW_KEY_UP,
                 GLFW.GLFW_KEY_DOWN,
                 GLFW.GLFW_KEY_LEFT,
                 GLFW.GLFW_KEY_RIGHT,
                 GLFW.GLFW_KEY_KP_ENTER,
                 GLFW.GLFW_KEY_KP_DIVIDE,
                 GLFW.GLFW_KEY_NUM_LOCK,
                 GLFW.GLFW_KEY_PRINT_SCREEN -> true;
            default -> false;
        };
    }

    private static CefKeyEvent createCefRawKeyEvent(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean pressed,
            char character,
            boolean numLockEnabled
    ) {
        char normalizedCharacter = normalizeCharacter(character);
        return new CefKeyEvent(
                pressed ? CefKeyEvent.KEYEVENT_RAWKEYDOWN : CefKeyEvent.KEYEVENT_KEYUP,
                toCefModifiers(modifiers, keyCode, numLockEnabled),
                resolveWindowsKeyCode(keyCode, scanCode, normalizedCharacter, numLockEnabled),
                resolveNativeKeyCode(keyCode, scanCode, normalizedCharacter, pressed),
                isSystemKey(modifiers),
                normalizedCharacter,
                normalizedCharacter,
                resolveScanCode(scanCode)
        );
    }

    private static CefKeyEvent createCefCharEvent(int keyCode, char character, int modifiers, boolean numLockEnabled) {
        char normalizedCharacter = normalizeCharacter(character);
        return new CefKeyEvent(
                CefKeyEvent.KEYEVENT_CHAR,
                toCefModifiers(modifiers, keyCode, numLockEnabled),
                normalizedCharacter,
                GraphenePlatform.isWindows() ? 0 : normalizedCharacter,
                isSystemKey(modifiers),
                normalizedCharacter,
                normalizedCharacter
        );
    }

    Component uiComponent() {
        return uiComponent;
    }

    @SuppressWarnings("MagicConstant")
    void mouseMoved(GrapheneBrowser browser, int x, int y, int modifiers) {
        int awtModifiers = toAwtModifiers(modifiers);
        MouseEvent event = new MouseEvent(uiComponent, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), awtModifiers, x, y, 0, false);
        browser.dispatchMouseEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    void mouseDragged(GrapheneBrowser browser, double x, double y, int button) {
        int awtModifiers = toAwtButtonDownModifier(button);
        MouseEvent event = new MouseEvent(uiComponent, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), awtModifiers, (int) x, (int) y, 1, true);
        browser.dispatchMouseEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    void mouseInteracted(GrapheneBrowser browser, int x, int y, int modifiers, int button, boolean pressed, int clickCount) {
        int awtButton = remapMouseCode(button);
        int awtModifiers = toAwtModifiers(modifiers);
        MouseEvent event = new MouseEvent(
                uiComponent,
                pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                awtModifiers,
                x,
                y,
                clickCount,
                false,
                awtButton
        );
        browser.dispatchMouseEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    void mouseScrolled(GrapheneBrowser browser, int x, int y, int modifiers, int amount, int rotation) {
        int awtModifiers = toAwtModifiers(modifiers);
        MouseWheelEvent event = new MouseWheelEvent(
                uiComponent,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                awtModifiers,
                x,
                y,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                amount,
                rotation
        );
        browser.dispatchMouseWheelEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    void keyTyped(GrapheneBrowser browser, char character, int modifiers) {
        ensureLockKeyModifiersEnabled();

        if (isDuplicateSyntheticTypedCharacter(character)) {
            GrapheneCore.LOGGER.info("[graphene-key] type-ignored duplicate char={} modifiers={} numLockKnown={} numLockFallback={}",
                    formatCharacter(character), modifiers, fallbackNumLockStateKnown, fallbackNumLockState);
            return;
        }

        if (character == KeyEvent.CHAR_UNDEFINED) {
            GrapheneCore.LOGGER.info("[graphene-key] type-ignored undefined modifiers={}", modifiers);
            return;
        }

        int charEventModifiers = sanitizeCharEventModifiers(modifiers);
        boolean numLockEnabled = isNumLockEnabled(modifiers);
        CefKeyEvent cefEvent = createCefCharEvent(GLFW.GLFW_KEY_UNKNOWN, character, charEventModifiers, numLockEnabled);
        logKeyInput("type", GLFW.GLFW_KEY_UNKNOWN, 0, charEventModifiers, character, numLockEnabled, false);
        logCefKeyEvent("dispatch-type", cefEvent);
        browser.dispatchCefKeyEvent(cefEvent);
    }

    @SuppressWarnings("MagicConstant")
    void keyEventByKeyCode(GrapheneBrowser browser, int keyCode, int scanCode, int modifiers, boolean pressed) {
        ensureLockKeyModifiersEnabled();
        if (keyCode == GLFW.GLFW_KEY_RIGHT_ALT) {
            rightAltPressed = pressed;
        }

        updateFallbackNumLockState(keyCode, pressed);

        char character = GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        boolean numLockEnabled = isNumLockEnabled(modifiers);
        boolean treatNumpadAsText = GrapheneKeyCodeUtil.isNumpadTextKey(keyCode)
                && shouldTreatNumpadAsText(keyCode, numLockEnabled);
        logKeyInput("key", keyCode, scanCode, modifiers, character, numLockEnabled, treatNumpadAsText);

        if (treatNumpadAsText) {
            dispatchNumpadTextKeyEvent(browser, keyCode, pressed, modifiers, scanCode, numLockEnabled);
            if (pressed) {
                dispatchSyntheticTypedCharacter(browser, keyCode, character, modifiers, numLockEnabled);
            }

            return;
        }

        CefKeyEvent cefEvent = createCefRawKeyEvent(
                keyCode,
                scanCode,
                modifiers,
                pressed,
                KeyEvent.CHAR_UNDEFINED,
                numLockEnabled
        );
        logCefKeyEvent("dispatch-raw", cefEvent);
        browser.dispatchCefKeyEvent(cefEvent);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            dispatchSyntheticTypedCharacter(browser, keyCode, '\b', modifiers, numLockEnabled);
        }

        if (pressed && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            dispatchSyntheticTypedCharacter(browser, keyCode, '\r', modifiers, numLockEnabled);
        }
    }

    @SuppressWarnings("MagicConstant")
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
        logCefKeyEvent("dispatch-numpad", cefEvent);
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
        CefKeyEvent cefEvent = createCefCharEvent(keyCode, character, modifiers, numLockEnabled);
        logCefKeyEvent("dispatch-synthetic-char", cefEvent);
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

    private static boolean isNumpadTextModeEnabled(int keyCode, boolean numLockEnabled) {
        return !GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) || numLockEnabled;
    }

    private boolean shouldTreatNumpadAsText(int keyCode, boolean numLockEnabled) {
        return isNumpadTextModeEnabled(keyCode, numLockEnabled);
    }

    private boolean isNumLockEnabled(int modifiers) {
        boolean numLockModifierSet = (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0;
        if (numLockModifierSet) {
            fallbackNumLockState = true;
            fallbackNumLockStateKnown = true;
            GrapheneCore.LOGGER.info("[graphene-key] numlock source=glfw-modifier value=true modifiers={}", modifiers);
            return true;
        }

        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            fallbackNumLockState = toolkitNumLockState.get();
            fallbackNumLockStateKnown = true;
            GrapheneCore.LOGGER.info("[graphene-key] numlock source=toolkit value={} modifiers={}", fallbackNumLockState, modifiers);
            return fallbackNumLockState;
        }

        if (fallbackNumLockStateKnown) {
            GrapheneCore.LOGGER.info("[graphene-key] numlock source=state-fallback value={} known=true modifiers={}",
                    fallbackNumLockState, modifiers);
            return fallbackNumLockState;
        }

        if (lockKeyModifiersEnabled) {
            GrapheneCore.LOGGER.info("[graphene-key] numlock source=glfw-lock-mods-fallback value=false modifiers={}", modifiers);
            return false;
        }

        GrapheneCore.LOGGER.info("[graphene-key] numlock source=unknown-default value=false modifiers={}", modifiers);
        return false;
    }

    private void updateFallbackNumLockState(int keyCode, boolean pressed) {
        if (!pressed || keyCode != GLFW.GLFW_KEY_NUM_LOCK) {
            return;
        }

        fallbackNumLockState = !fallbackNumLockState;
        fallbackNumLockStateKnown = true;
        GrapheneCore.LOGGER.info("[graphene-key] numlock toggled by key event newValue={}", fallbackNumLockState);
    }

    private void ensureLockKeyModifiersEnabled() {
        if (lockKeyModifiersEnabled) {
            return;
        }

        try {
            long windowHandle = McClient.mc().getWindow().handle();
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_LOCK_KEY_MODS, GLFW.GLFW_TRUE);
            lockKeyModifiersEnabled = true;
        } catch (Exception _) {
            // Lock key modifiers are optional at runtime.
        }
    }

    private static Optional<Boolean> readToolkitNumLockState() {
        try {
            return Optional.of(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK));
        } catch (Exception _) {
            // Toolkit lock state is not available on all platforms/toolkits.
            return Optional.empty();
        }
    }

    private int sanitizeCharEventModifiers(int modifiers) {
        boolean hasControlModifier = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean hasAltModifier = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        if (GraphenePlatform.isWindows() && rightAltPressed && hasControlModifier && hasAltModifier) {
            return modifiers & ~(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT);
        }

        return modifiers;
    }

    private void logKeyInput(
            String phase,
            int keyCode,
            int scanCode,
            int modifiers,
            char character,
            boolean numLockEnabled,
            boolean treatNumpadAsText
    ) {
        GrapheneCore.LOGGER.info(
                "[graphene-key] {} keyCode={} scanCode={} modifiers={} char={} numLockEnabled={} numLockKnown={} numLockFallback={} treatNumpadAsText={}",
                phase,
                keyCode,
                scanCode,
                modifiers,
                formatCharacter(character),
                numLockEnabled,
                fallbackNumLockStateKnown,
                fallbackNumLockState,
                treatNumpadAsText
        );
    }

    private static void logCefKeyEvent(String phase, CefKeyEvent event) {
        GrapheneCore.LOGGER.info(
                "[graphene-key] {} type={} modifiers={} windowsKey={} nativeKey={} systemKey={} char={} unmodifiedChar={} scanCode={}",
                phase,
                event.type,
                event.modifiers,
                event.windows_key_code,
                event.native_key_code,
                event.is_system_key,
                formatCharacter(event.character),
                formatCharacter(event.unmodified_character),
                event.scan_code
        );
    }

    private static String formatCharacter(char character) {
        return "'" + character + "'(U+" + String.format("%04X", (int) character) + ")";
    }
}
