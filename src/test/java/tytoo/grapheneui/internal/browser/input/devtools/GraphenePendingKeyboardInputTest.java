package tytoo.grapheneui.internal.browser.input.devtools;

import org.junit.jupiter.api.Test;
import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyData;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphenePendingKeyboardInputTest {
    @Test
    void pendingKeyDownCanBeStoredAndCleared() {
        GraphenePendingKeyboardInput pendingInput = new GraphenePendingKeyboardInput();
        GraphenePendingKeyboardInput.PendingKeyDown keyDown = new GraphenePendingKeyboardInput.PendingKeyDown(
                65,
                30,
                0,
                false,
                new GrapheneDomKeyData("KeyA", "a", 65, 30, 0, false, false, 0),
                false
        );

        pendingInput.setKeyDown(keyDown);

        assertTrue(pendingInput.hasKeyDown());
        assertTrue(pendingInput.isKeyDownPending(65));
        assertFalse(pendingInput.isKeyDownPending(66));
        assertSame(keyDown, pendingInput.clearKeyDown());
        assertFalse(pendingInput.hasKeyDown());
    }

    @Test
    void pendingKeyUpCanBeStoredAndCleared() {
        GraphenePendingKeyboardInput pendingInput = new GraphenePendingKeyboardInput();
        GraphenePendingKeyboardInput.PendingKeyUp keyUp = new GraphenePendingKeyboardInput.PendingKeyUp(65, 30, 0, false);

        pendingInput.setKeyUp(keyUp);

        assertTrue(pendingInput.hasKeyUp());
        assertSame(keyUp, pendingInput.clearKeyUp());
        assertFalse(pendingInput.hasKeyUp());
    }

    @Test
    void syntheticTextDuplicateIsDetectedOnceWithinWindow() {
        GraphenePendingKeyboardInput pendingInput = new GraphenePendingKeyboardInput();

        pendingInput.rememberSyntheticText("1");

        assertTrue(pendingInput.isDuplicateSyntheticText("1"));
        assertFalse(pendingInput.isDuplicateSyntheticText("1"));
    }

    @Test
    void differentSyntheticTextIsNotDuplicate() {
        GraphenePendingKeyboardInput pendingInput = new GraphenePendingKeyboardInput();

        pendingInput.rememberSyntheticText("1");

        assertFalse(pendingInput.isDuplicateSyntheticText("2"));
    }

    @Test
    void resetClearsPendingState() {
        GraphenePendingKeyboardInput pendingInput = new GraphenePendingKeyboardInput();
        pendingInput.setKeyUp(new GraphenePendingKeyboardInput.PendingKeyUp(65, 30, 0, false));
        pendingInput.rememberSyntheticText("1");

        pendingInput.reset();

        assertFalse(pendingInput.hasKeyDown());
        assertFalse(pendingInput.hasKeyUp());
        assertFalse(pendingInput.isDuplicateSyntheticText("1"));
    }
}
