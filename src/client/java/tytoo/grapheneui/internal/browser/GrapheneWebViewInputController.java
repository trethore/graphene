package tytoo.grapheneui.internal.browser;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.awt.*;
import java.util.Objects;

public final class GrapheneWebViewInputController {
    private static final int MAX_CLICK_COUNT = 3;

    private final GrapheneBrowser browser;
    private final GrapheneFocusUtil focusUtil;
    private int lastBrowserMouseX = Integer.MIN_VALUE;
    private int lastBrowserMouseY = Integer.MIN_VALUE;
    private boolean primaryPointerButtonDown;
    private int lastClickButton = -1;
    private int clickCount;
    private int pressedButton = -1;
    private int pressedClickCount = 1;

    public GrapheneWebViewInputController(GrapheneBrowser browser, GrapheneFocusUtil focusUtil) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.focusUtil = Objects.requireNonNull(focusUtil, "focusUtil");
    }

    public boolean isPrimaryPointerButtonDown() {
        return primaryPointerButtonDown;
    }

    public void updateMousePosition(Point browserPoint) {
        if (browserPoint.x == lastBrowserMouseX && browserPoint.y == lastBrowserMouseY) {
            return;
        }

        lastBrowserMouseX = browserPoint.x;
        lastBrowserMouseY = browserPoint.y;
        browser.mouseMoved(browserPoint.x, browserPoint.y, 0);
    }

    public void onMouseClicked(int button, boolean isDoubleClick, Point browserPoint) {
        primaryPointerButtonDown = button == 0;
        int currentClickCount = resolveClickCount(button, isDoubleClick);
        pressedButton = button;
        pressedClickCount = currentClickCount;
        browser.mouseInteracted(browserPoint.x, browserPoint.y, 0, button, true, currentClickCount);
    }

    public boolean onMouseReleased(int button, Point browserPoint) {
        if (button == 0) {
            primaryPointerButtonDown = false;
        }

        if (!focusUtil.isFocused()) {
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

    public boolean onMouseDragged(int button, Point browserPoint) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        lastBrowserMouseX = browserPoint.x;
        lastBrowserMouseY = browserPoint.y;
        browser.mouseDragged(browserPoint.x, browserPoint.y, button);
        return true;
    }

    public void onMouseScrolled(Point browserPoint, int delta, int rotation) {
        browser.mouseScrolled(browserPoint.x, browserPoint.y, 0, delta, rotation);
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), true);
    }

    public void onKeyReleased(KeyEvent keyEvent) {
        browser.keyEventByKeyCode(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers(), false);
    }

    public void onCharacterTyped(CharacterEvent characterEvent) {
        browser.keyTyped((char) characterEvent.codepoint(), characterEvent.modifiers());
    }

    public void onFocusChanged(boolean focused) {
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
