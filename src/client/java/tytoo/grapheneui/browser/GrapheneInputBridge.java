package tytoo.grapheneui.browser;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.input.GrapheneKeyCodeUtil;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

final class GrapheneInputBridge {
    private static final int MOUSE_LEFT_BUTTON = 0;
    private static final int MOUSE_RIGHT_BUTTON = 1;
    private static final int MOUSE_MIDDLE_BUTTON = 2;

    private final GrapheneScancodeInjector scancodeInjector = GrapheneScancodeInjector.create();
    private final Component uiComponent = new Component() {
    };

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
        int awtModifiers = toAwtModifiers(modifiers);
        KeyEvent event = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, character);
        browser.dispatchKeyEvent(event);
    }

    @SuppressWarnings("MagicConstant")
    void keyEventByKeyCode(GrapheneBrowser browser, int keyCode, int scanCode, int modifiers, boolean pressed) {
        int awtModifiers = toAwtModifiers(modifiers);
        int awtKeyCode = GrapheneKeyCodeUtil.toAwtKeyCode(keyCode);
        char character = GrapheneKeyCodeUtil.toCharacter(keyCode, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);

        KeyEvent event = new KeyEvent(
                uiComponent,
                pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                awtModifiers,
                awtKeyCode,
                character == KeyEvent.CHAR_UNDEFINED ? 0 : character,
                KeyEvent.KEY_LOCATION_STANDARD
        );

        scancodeInjector.inject(event, scanCode);
        browser.dispatchKeyEvent(event);

        if (pressed && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            KeyEvent typedBackspace = new KeyEvent(uiComponent, KeyEvent.KEY_TYPED, System.currentTimeMillis(), awtModifiers, 0, '\b');
            scancodeInjector.inject(typedBackspace, scanCode);
            browser.dispatchKeyEvent(typedBackspace);
        }
    }
}
