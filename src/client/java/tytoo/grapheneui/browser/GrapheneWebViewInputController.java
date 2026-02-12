package tytoo.grapheneui.browser;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.awt.*;
import java.util.Objects;

final class GrapheneWebViewInputController {
    private static final int MAX_CLICK_COUNT = 3;

    private final GrapheneBrowser browser;
    private int lastBrowserMouseX = Integer.MIN_VALUE;
    private int lastBrowserMouseY = Integer.MIN_VALUE;
    private boolean primaryPointerButtonDown;
    private int lastClickButton = -1;
    private int clickCount;
    private int pressedButton = -1;
    private int pressedClickCount = 1;

    GrapheneWebViewInputController(GrapheneBrowser browser) {
        this.browser = Objects.requireNonNull(browser, "browser");
    }

    boolean isPrimaryPointerButtonDown() {
        return primaryPointerButtonDown;
    }

    void updateMousePosition(Point browserPoint) {
        if (browserPoint.x == lastBrowserMouseX && browserPoint.y == lastBrowserMouseY) {
            return;
        }

        lastBrowserMouseX = browserPoint.x;
        lastBrowserMouseY = browserPoint.y;
        browser.mouseMoved(browserPoint.x, browserPoint.y, 0);
    }

    void onMouseClicked(int button, boolean isDoubleClick, Point browserPoint) {
        primaryPointerButtonDown = button == 0;
        int currentClickCount = resolveClickCount(button, isDoubleClick);
        pressedButton = button;
        pressedClickCount = currentClickCount;
        browser.mouseInteracted(browserPoint.x, browserPoint.y, 0, button, true, currentClickCount);
    }

    boolean onMouseReleased(int button, Point browserPoint, boolean focused) {
        if (button == 0) {
            primaryPointerButtonDown = false;
        }

        if (!focused) {
            return false;
        }

        int releaseClickCount = button == pressedButton ? pressedClickCount : 1;
        browser.mouseInteracted(browserPoint.x, browserPoint.y, 0, button, false, releaseClickCount);
        if (button == pressedButton) {
            pressedButton = -1;
            pressedClickCount = 1;
        }

        return true;
    }

    boolean onMouseDragged(int button, Point browserPoint, boolean focused) {
        if (!focused) {
            return false;
        }

        lastBrowserMouseX = browserPoint.x;
        lastBrowserMouseY = browserPoint.y;
        browser.mouseDragged(browserPoint.x, browserPoint.y, button);
        return true;
    }

    void onMouseScrolled(Point browserPoint, int delta, int rotation) {
        browser.mouseScrolled(browserPoint.x, browserPoint.y, 0, delta, rotation);
    }

    void onKeyPressed(KeyEvent keyEvent) {
        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), true);
    }

    void onKeyReleased(KeyEvent keyEvent) {
        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), false);
    }

    void onCharacterTyped(CharacterEvent characterEvent) {
        browser.keyTyped((char) characterEvent.codepoint(), characterEvent.modifiers());
    }

    void onFocusChanged(boolean focused) {
        if (focused) {
            return;
        }

        primaryPointerButtonDown = false;
        pressedButton = -1;
        pressedClickCount = 1;
    }

    private int resolveClickCount(int button, boolean isDoubleClick) {
        if (!isDoubleClick || button != lastClickButton) {
            clickCount = 1;
        } else {
            clickCount = Math.min(clickCount + 1, MAX_CLICK_COUNT);
        }

        lastClickButton = button;
        return clickCount;
    }
}
