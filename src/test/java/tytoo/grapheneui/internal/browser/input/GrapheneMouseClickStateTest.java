package tytoo.grapheneui.internal.browser.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneMouseClickStateTest {
    @Test
    void firstPressStartsSingleClick() {
        GrapheneMouseClickState clickState = new GrapheneMouseClickState();

        int clickCount = clickState.press(0, false, 1);

        assertEquals(1, clickCount);
        assertEquals(1, clickState.pressedButtons());
    }

    @Test
    void doubleClickSameButtonIncrementsAndCapsAtThree() {
        GrapheneMouseClickState clickState = new GrapheneMouseClickState();

        assertEquals(1, clickState.press(0, false, 1));
        assertEquals(2, clickState.press(0, true, 1));
        assertEquals(3, clickState.press(0, true, 1));
        assertEquals(3, clickState.press(0, true, 1));
    }

    @Test
    void differentButtonResetsClickCount() {
        GrapheneMouseClickState clickState = new GrapheneMouseClickState();

        clickState.press(0, false, 1);
        clickState.press(0, true, 1);

        assertEquals(1, clickState.press(1, true, 2));
    }

    @Test
    void releaseUsesClickCountFromMatchingPress() {
        GrapheneMouseClickState clickState = new GrapheneMouseClickState();

        clickState.press(0, false, 1);
        int clickCount = clickState.press(0, true, 1);

        assertEquals(clickCount, clickState.releaseClickCount(0));
        assertEquals(1, clickState.releaseClickCount(1));
    }

    @Test
    void pressedButtonsTracksReleasedBits() {
        GrapheneMouseClickState clickState = new GrapheneMouseClickState();

        clickState.press(0, false, 1);
        clickState.press(1, false, 2);
        clickState.releaseButtonBit(1);

        assertEquals(2, clickState.pressedButtons());
    }

    @Test
    void resetClearsPressedState() {
        GrapheneMouseClickState clickState = new GrapheneMouseClickState();

        clickState.press(0, false, 1);
        clickState.reset();

        assertEquals(0, clickState.pressedButtons());
        assertEquals(1, clickState.releaseClickCount(0));
    }
}
