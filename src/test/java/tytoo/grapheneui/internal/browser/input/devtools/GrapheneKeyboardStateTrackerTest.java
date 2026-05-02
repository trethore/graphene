package tytoo.grapheneui.internal.browser.input.devtools;

import org.junit.jupiter.api.Test;
import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyData;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneKeyboardStateTrackerTest {
    @Test
    void markPressedReturnsWhetherKeyIsRepeated() {
        GrapheneKeyboardStateTracker tracker = new GrapheneKeyboardStateTracker();

        assertFalse(tracker.markPressed(65));
        assertTrue(tracker.markPressed(65));

        tracker.markReleased(65);

        assertFalse(tracker.markPressed(65));
    }

    @Test
    void activeKeyDataCanBeRememberedAndCleared() {
        GrapheneKeyboardStateTracker tracker = new GrapheneKeyboardStateTracker();
        GrapheneDomKeyData keyData = new GrapheneDomKeyData("KeyA", "a", 65, 30, 0, false, false, 0);

        tracker.rememberActiveKeyData(65, keyData);

        assertSame(keyData, tracker.clearActiveKeyData(65));
        assertNull(tracker.clearActiveKeyData(65));
    }

    @Test
    void resetClearsPressedAndActiveKeyData() {
        GrapheneKeyboardStateTracker tracker = new GrapheneKeyboardStateTracker();
        GrapheneDomKeyData keyData = new GrapheneDomKeyData("KeyA", "a", 65, 30, 0, false, false, 0);

        tracker.markPressed(65);
        tracker.rememberActiveKeyData(65, keyData);
        tracker.reset();

        assertFalse(tracker.markPressed(65));
        assertNull(tracker.clearActiveKeyData(65));
    }
}
