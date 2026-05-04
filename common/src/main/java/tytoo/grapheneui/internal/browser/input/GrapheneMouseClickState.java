package tytoo.grapheneui.internal.browser.input;

public final class GrapheneMouseClickState {
    private static final int MAX_CLICK_COUNT = 3;

    private int lastClickButton = -1;
    private int clickCount;
    private int pressedButton = -1;
    private int pressedClickCount = 1;
    private int pressedButtons;

    public int press(int button, boolean doubleClick, int buttonBit) {
        int currentClickCount = resolveClickCount(button, doubleClick);
        pressedButton = button;
        pressedClickCount = currentClickCount;
        pressedButtons |= buttonBit;
        return currentClickCount;
    }

    public void releaseButtonBit(int buttonBit) {
        pressedButtons &= ~buttonBit;
    }

    public int releaseClickCount(int button) {
        return button == pressedButton ? pressedClickCount : 1;
    }

    public void clearPressedButton(int button) {
        if (button != pressedButton) {
            return;
        }

        pressedButton = -1;
        pressedClickCount = 1;
    }

    public int pressedButtons() {
        return pressedButtons;
    }

    public void reset() {
        pressedButton = -1;
        pressedClickCount = 1;
        pressedButtons = 0;
    }

    private int resolveClickCount(int button, boolean doubleClick) {
        if (!doubleClick || button != lastClickButton) {
            clickCount = 1;
        } else {
            clickCount = Math.min(clickCount + 1, MAX_CLICK_COUNT);
        }

        lastClickButton = button;
        return clickCount;
    }
}
