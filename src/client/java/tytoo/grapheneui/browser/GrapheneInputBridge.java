package tytoo.grapheneui.browser;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.input.GrapheneKeyCodeUtil;
import tytoo.grapheneui.mc.McClient;

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

    private final GrapheneScancodeInjector scancodeInjector = GrapheneScancodeInjector.create();
    private final Component uiComponent = new Component() {
    };
    private char pendingSyntheticCharacter = KeyEvent.CHAR_UNDEFINED;
    private long pendingSyntheticCharacterTimestamp;
    private boolean fallbackNumLockState;
    private boolean fallbackNumLockStateKnown;

    GrapheneInputBridge() {
        enableLockKeyModifiers();
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

    private static int toAwtKeyLocation(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT,
                 GLFW.GLFW_KEY_LEFT_CONTROL,
                 GLFW.GLFW_KEY_LEFT_ALT,
                 GLFW.GLFW_KEY_LEFT_SUPER -> KeyEvent.KEY_LOCATION_LEFT;
            case GLFW.GLFW_KEY_RIGHT_SHIFT,
                 GLFW.GLFW_KEY_RIGHT_CONTROL,
                 GLFW.GLFW_KEY_RIGHT_ALT,
                 GLFW.GLFW_KEY_RIGHT_SUPER -> KeyEvent.KEY_LOCATION_RIGHT;
            default -> GrapheneKeyCodeUtil.isNumpadKey(keyCode) ? KeyEvent.KEY_LOCATION_NUMPAD : KeyEvent.KEY_LOCATION_STANDARD;
        };
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
        if (isDuplicateSyntheticTypedCharacter(character)) {
            return;
        }

        int awtModifiers = toAwtModifiers(modifiers);
        KeyEvent event = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, character);
        browser.dispatchKeyEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    void keyEventByKeyCode(GrapheneBrowser browser, int keyCode, int scanCode, int modifiers, boolean pressed) {
        updateFallbackNumLockState(keyCode, pressed);

        int awtModifiers = toAwtModifiers(modifiers);
        int awtKeyCode = GrapheneKeyCodeUtil.toAwtKeyCode(keyCode);
        char character = GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
        int keyLocation = toAwtKeyLocation(keyCode);
        boolean numLockEnabled = isNumLockEnabled(modifiers);

        if (GrapheneKeyCodeUtil.isNumpadTextKey(keyCode) && isNumpadTextModeEnabled(keyCode, numLockEnabled)) {
            dispatchNumpadTextKeyEvent(browser, keyCode, pressed, awtModifiers, character, scanCode);
            if (pressed) {
                dispatchSyntheticTypedFromNumpad(browser, character, awtModifiers);
            }

            return;
        }

        if (GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) && !numLockEnabled) {
            character = KeyEvent.CHAR_UNDEFINED;
        }

        KeyEvent event = new KeyEvent(
                uiComponent,
                pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                awtModifiers,
                awtKeyCode,
                character == KeyEvent.CHAR_UNDEFINED ? 0 : character,
                keyLocation
        );

        scancodeInjector.inject(event, scanCode);
        browser.dispatchKeyEvent(event);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            KeyEvent typedBackspace = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, '\b');
            browser.dispatchKeyEvent(typedBackspace);
        }

        if (pressed && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            KeyEvent typedEnter = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, '\r');
            browser.dispatchKeyEvent(typedEnter);
        }
    }

    @SuppressWarnings("MagicConstant")
    private void dispatchNumpadTextKeyEvent(
            GrapheneBrowser browser,
            int glfwKeyCode,
            boolean pressed,
            int awtModifiers,
            char character,
            int scanCode
    ) {
        int awtKeyCode = GrapheneKeyCodeUtil.toAwtKeyCode(glfwKeyCode);
        char eventCharacter = pressed ? character : KeyEvent.CHAR_UNDEFINED;
        KeyEvent event = new KeyEvent(
                uiComponent,
                pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                awtModifiers,
                awtKeyCode,
                eventCharacter == KeyEvent.CHAR_UNDEFINED ? 0 : eventCharacter,
                KeyEvent.KEY_LOCATION_NUMPAD
        );

        if (GrapheneKeyCodeUtil.isNumpadOperatorKey(glfwKeyCode)) {
            scancodeInjector.inject(event, scanCode);
        }

        browser.dispatchKeyEvent(event);
    }

    private void dispatchSyntheticTypedFromNumpad(GrapheneBrowser browser, char character, int awtModifiers) {
        if (character == KeyEvent.CHAR_UNDEFINED) {
            return;
        }

        rememberSyntheticTypedCharacter(character);
        KeyEvent typed = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, character);
        browser.dispatchKeyEvent(typed);
    }

    private void rememberSyntheticTypedCharacter(char character) {
        pendingSyntheticCharacter = character;
        pendingSyntheticCharacterTimestamp = System.currentTimeMillis();
    }

    private boolean isDuplicateSyntheticTypedCharacter(char character) {
        if (pendingSyntheticCharacter == KeyEvent.CHAR_UNDEFINED) {
            return false;
        }

        long now = System.currentTimeMillis();
        boolean duplicate = now - pendingSyntheticCharacterTimestamp <= SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS
                && pendingSyntheticCharacter == character;
        pendingSyntheticCharacter = KeyEvent.CHAR_UNDEFINED;
        pendingSyntheticCharacterTimestamp = 0L;
        return duplicate;
    }

    private static boolean isNumpadTextModeEnabled(int keyCode, boolean numLockEnabled) {
        return !GrapheneKeyCodeUtil.requiresNumLockForText(keyCode) || numLockEnabled;
    }

    private boolean isNumLockEnabled(int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0) {
            fallbackNumLockState = true;
            fallbackNumLockStateKnown = true;
            return true;
        }

        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            fallbackNumLockState = toolkitNumLockState.get();
            fallbackNumLockStateKnown = true;
            return fallbackNumLockState;
        }

        return fallbackNumLockStateKnown && fallbackNumLockState;
    }

    private void updateFallbackNumLockState(int keyCode, boolean pressed) {
        if (!pressed || keyCode != GLFW.GLFW_KEY_NUM_LOCK) {
            return;
        }

        fallbackNumLockState = !fallbackNumLockState;
        fallbackNumLockStateKnown = true;
    }

    private static void enableLockKeyModifiers() {
        try {
            long windowHandle = McClient.mc().getWindow().handle();
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_LOCK_KEY_MODS, GLFW.GLFW_TRUE);
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
}
