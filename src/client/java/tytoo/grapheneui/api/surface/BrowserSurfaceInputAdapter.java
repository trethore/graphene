package tytoo.grapheneui.api.surface;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import tytoo.grapheneui.internal.browser.GrapheneFocusUtil;
import tytoo.grapheneui.internal.browser.GrapheneWebViewInputController;

import java.awt.*;
import java.util.Objects;

/**
 * Adapter class that translates input events from a {@link BrowserSurface} to the underlying browser instance.
 * It handles mouse movement, clicks, scrolls, and keyboard events, ensuring they are correctly forwarded to the browser.
 * The adapter also manages focus state to ensure input is only sent when the surface is focused.
 */
public final class BrowserSurfaceInputAdapter {
    private static final int WHEEL_AMOUNT_PER_STEP = 120;

    private final BrowserSurface surface;
    private final GrapheneFocusUtil focusUtil;
    private final GrapheneWebViewInputController inputController;
    private double pendingWheelAmount;

    public BrowserSurfaceInputAdapter(BrowserSurface surface) {
        this.surface = Objects.requireNonNull(surface, "surface");
        this.focusUtil = new GrapheneFocusUtil(this.surface.internalBrowser()::setFocus);
        this.inputController = new GrapheneWebViewInputController(this.surface.internalBrowser(), this.focusUtil, this.surface.bridge());
        this.focusUtil.addFocusListener(this.inputController::onFocusChanged);
        this.focusUtil.syncNativeFocus();
    }

    public boolean isFocused() {
        return focusUtil.isFocused();
    }

    public void setFocused(boolean focused) {
        focusUtil.setFocused(focused);
    }

    public boolean isPrimaryPointerButtonDown() {
        return inputController.isPrimaryPointerButtonDown();
    }

    public void mouseMoved(double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        inputController.updateMousePosition(toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight));
    }

    public void mouseClicked(int button, boolean isDoubleClick, double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        inputController.onMouseClicked(button, isDoubleClick, toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight));
    }

    public boolean mouseReleased(int button, double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        return inputController.onMouseReleased(button, toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight));
    }

    public boolean mouseDragged(int button, double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        return inputController.onMouseDragged(button, toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight));
    }

    public void mouseScrolled(double surfaceX, double surfaceY, int amount, int rotation, int renderedWidth, int renderedHeight) {
        inputController.onMouseScrolled(
                toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight),
                amount,
                rotation
        );
    }

    public void mouseScrolled(double surfaceX, double surfaceY, double scrollY, int renderedWidth, int renderedHeight) {
        double preciseAmount = scrollY * WHEEL_AMOUNT_PER_STEP + pendingWheelAmount;
        int amount = (int) preciseAmount;
        pendingWheelAmount = preciseAmount - amount;
        if (amount == 0) {
            return;
        }

        mouseScrolled(surfaceX, surfaceY, amount, 1, renderedWidth, renderedHeight);
    }

    public boolean keyPressed(KeyEvent keyEvent) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        inputController.onKeyPressed(keyEvent);
        return true;
    }

    public boolean keyReleased(KeyEvent keyEvent) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        inputController.onKeyReleased(keyEvent);
        return true;
    }

    public boolean charTyped(CharacterEvent characterEvent) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        inputController.onCharacterTyped(characterEvent);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        surface.internalBrowser().keyEventByKeyCode(keyCode, scanCode, modifiers, true);
        return true;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        surface.internalBrowser().keyEventByKeyCode(keyCode, scanCode, modifiers, false);
        return true;
    }

    public boolean charTyped(int codePoint, int modifiers) {
        if (!focusUtil.isFocused()) {
            return false;
        }

        surface.internalBrowser().keyTyped((char) codePoint, modifiers);
        return true;
    }

    private Point toBrowserPoint(double surfaceX, double surfaceY, int renderedWidth, int renderedHeight) {
        return surface.toBrowserPoint(surfaceX, surfaceY, renderedWidth, renderedHeight);
    }
}
