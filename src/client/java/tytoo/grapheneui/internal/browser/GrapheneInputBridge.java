package tytoo.grapheneui.internal.browser;

import org.cef.input.CefMouseEvent;
import org.cef.input.CefMouseWheelEvent;
import org.cef.misc.EventFlags;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.keyboard.GrapheneKeyboardInputBridge;

import java.awt.*;

final class GrapheneInputBridge {
    private static final int MOUSE_LEFT_BUTTON = 0;
    private static final int MOUSE_RIGHT_BUTTON = 1;
    private static final int MOUSE_MIDDLE_BUTTON = 2;

    private final Component uiComponent = new Component() {
    };
    private final GrapheneKeyboardInputBridge keyboardInputBridge = new GrapheneKeyboardInputBridge();

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
        keyboardInputBridge.keyTyped(browser::dispatchCefKeyEvent, character, modifiers);
    }

    void keyEventByKeyCode(GrapheneBrowser browser, int keyCode, int scanCode, int modifiers, boolean pressed) {
        keyboardInputBridge.keyEventByKeyCode(browser::dispatchCefKeyEvent, keyCode, scanCode, modifiers, pressed);
    }
}
