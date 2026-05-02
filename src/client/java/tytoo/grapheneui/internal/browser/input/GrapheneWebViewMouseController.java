package tytoo.grapheneui.internal.browser.input;

import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.browser.GrapheneFocusUtil;
import tytoo.grapheneui.internal.input.GrapheneInputModifierUtil;
import tytoo.grapheneui.internal.input.mouse.GrapheneMouseButtonUtil;

import java.awt.*;
import java.util.Objects;

final class GrapheneWebViewMouseController {
    private final GrapheneBrowser browser;
    private final GrapheneFocusUtil focusUtil;
    private final GrapheneMouseClickState clickState = new GrapheneMouseClickState();
    private final GrapheneWheelZoomController wheelZoomController = new GrapheneWheelZoomController();
    private final GrapheneExtraMouseButtonEmitter extraMouseButtonEmitter;

    private int lastBrowserMouseX = Integer.MIN_VALUE;
    private int lastBrowserMouseY = Integer.MIN_VALUE;
    private boolean primaryPointerButtonDown;

    GrapheneWebViewMouseController(GrapheneBrowser browser, GrapheneFocusUtil focusUtil, GrapheneBridge bridge) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.focusUtil = Objects.requireNonNull(focusUtil, "focusUtil");
        this.extraMouseButtonEmitter = new GrapheneExtraMouseButtonEmitter(bridge);
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

    void mouseClicked(int button, boolean doubleClick, Point browserPoint) {
        primaryPointerButtonDown = button == 0;
        int buttonBit = GrapheneMouseButtonUtil.toDevToolsButtonsBit(button);
        int clickCount = clickState.press(button, doubleClick, buttonBit);
        int modifiers = GrapheneInputModifierUtil.currentModifiers();
        if (GrapheneMouseButtonUtil.isBrowserNavigationButton(button)) {
            browser.navigationButtonInteracted(browserPoint.x, browserPoint.y, modifiers, button, true, clickCount, clickState.pressedButtons());
            return;
        }

        if (GrapheneMouseButtonUtil.isExtraMouseButton(button)) {
            extraMouseButtonEmitter.emit(button, true);
            return;
        }

        browser.mouseInteracted(browserPoint.x, browserPoint.y, modifiers, button, true, clickCount);
    }

    boolean mouseReleased(int button, Point browserPoint) {
        if (button == 0) {
            primaryPointerButtonDown = false;
        }

        clickState.releaseButtonBit(GrapheneMouseButtonUtil.toDevToolsButtonsBit(button));
        int modifiers = GrapheneInputModifierUtil.currentModifiers();
        int cefModifiers = GrapheneInputModifierUtil.toCefCommonModifiers(modifiers);

        if (!focusUtil.isFocused()) {
            if (button == 0) {
                browser.cancelActiveDrag();
            }
            return false;
        }

        int clickCount = clickState.releaseClickCount(button);
        if (GrapheneMouseButtonUtil.isBrowserNavigationButton(button)) {
            browser.navigationButtonInteracted(browserPoint.x, browserPoint.y, modifiers, button, false, clickCount, clickState.pressedButtons());
        } else if (GrapheneMouseButtonUtil.isExtraMouseButton(button)) {
            extraMouseButtonEmitter.emit(button, false);
        } else {
            browser.mouseInteracted(browserPoint.x, browserPoint.y, modifiers, button, false, clickCount);
            if (button == 0) {
                browser.dragCompleted(browserPoint.x, browserPoint.y, cefModifiers);
            }
        }

        clickState.clearPressedButton(button);
        return true;
    }

    boolean mouseDragged(int button, Point browserPoint) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        lastBrowserMouseX = browserPoint.x;
        lastBrowserMouseY = browserPoint.y;
        browser.mouseDragged(browserPoint.x, browserPoint.y, button);
        browser.dragUpdated(browserPoint.x, browserPoint.y, currentCefModifiers());
        return true;
    }

    void mouseScrolled(Point browserPoint, int delta, int rotation) {
        int modifiers = GrapheneInputModifierUtil.currentModifiers();
        int wheelDelta = delta * rotation;
        if (GrapheneInputModifierUtil.isEditShortcutModifierDown(modifiers) && wheelDelta != 0) {
            applyZoomDelta(wheelDelta);
            return;
        }

        browser.mouseScrolled(browserPoint.x, browserPoint.y, modifiers, delta, rotation);
    }

    void reset() {
        browser.cancelActiveDrag();
        extraMouseButtonEmitter.reset();
        primaryPointerButtonDown = false;
        clickState.reset();
    }

    private void applyZoomDelta(int wheelDelta) {
        double currentZoomLevel = browser.getZoomLevel();
        double nextZoomLevel = wheelZoomController.applyZoomDelta(currentZoomLevel, wheelDelta);
        if (Double.compare(nextZoomLevel, currentZoomLevel) == 0) {
            return;
        }

        browser.setZoomLevel(nextZoomLevel);
    }

    private int currentCefModifiers() {
        return GrapheneInputModifierUtil.toCefCommonModifiers(GrapheneInputModifierUtil.currentModifiers());
    }
}
