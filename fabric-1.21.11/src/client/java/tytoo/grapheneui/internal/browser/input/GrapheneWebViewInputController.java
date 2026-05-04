package tytoo.grapheneui.internal.browser.input;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.browser.GrapheneFocusUtil;

import java.awt.*;
import java.util.Objects;

public final class GrapheneWebViewInputController {
    private final GrapheneWebViewMouseController mouseController;
    private final GrapheneWebViewKeyboardController keyboardController;

    public GrapheneWebViewInputController(GrapheneBrowser browser, GrapheneFocusUtil focusUtil, GrapheneBridge bridge) {
        Objects.requireNonNull(browser, "browser");
        this.mouseController = new GrapheneWebViewMouseController(browser, focusUtil, bridge);
        this.keyboardController = new GrapheneWebViewKeyboardController(browser);
    }

    public boolean isPrimaryPointerButtonDown() {
        return mouseController.isPrimaryPointerButtonDown();
    }

    public void updateMousePosition(Point browserPoint) {
        mouseController.updateMousePosition(browserPoint);
    }

    public void onMouseClicked(int button, boolean doubleClick, Point browserPoint) {
        mouseController.mouseClicked(button, doubleClick, browserPoint);
    }

    public boolean onMouseReleased(int button, Point browserPoint) {
        return mouseController.mouseReleased(button, browserPoint);
    }

    public boolean onMouseDragged(int button, Point browserPoint) {
        return mouseController.mouseDragged(button, browserPoint);
    }

    public void onMouseScrolled(Point browserPoint, int delta, int rotation) {
        mouseController.mouseScrolled(browserPoint, delta, rotation);
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        keyboardController.keyPressed(keyEvent);
    }

    public void onKeyReleased(KeyEvent keyEvent) {
        keyboardController.keyReleased(keyEvent);
    }

    public void onCharacterTyped(CharacterEvent characterEvent) {
        keyboardController.characterTyped(characterEvent);
    }

    public void onFocusChanged(boolean focused) {
        if (focused) {
            return;
        }

        mouseController.reset();
        keyboardController.reset();
    }
}
