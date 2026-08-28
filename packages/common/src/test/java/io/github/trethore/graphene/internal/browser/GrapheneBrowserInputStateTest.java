package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserPointerAction;
import io.github.trethore.graphene.api.browser.input.BrowserPointerButton;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class GrapheneBrowserInputStateTest {
    @Test
    void normalizesSupportedControlsAndFiltersUnsupportedControls() {
        assertEquals("a\b\rb", GrapheneBrowserInputState.normalizeText("a\u007F\n\uF700\u0001b"));
        assertNull(GrapheneBrowserInputState.normalizeText("\uF700\u0001"));
    }

    @Test
    void preservesUnchangedUnicodeText() {
        String text = "\uD83D\uDE00e\u0301";

        assertSame(text, GrapheneBrowserInputState.normalizeText(text));
    }

    @Test
    void suppressesCommittedTextFollowingSyntheticText() {
        GrapheneBrowserInputState state = new GrapheneBrowserInputState();

        state.syntheticText("\r", Set.of(), 1_000);

        assertNull(state.committedText("\r", Set.of(), 1_100));
    }

    @Test
    void removesAltGrControlAndAltModifiersFromText() {
        GrapheneBrowserInputState state = new GrapheneBrowserInputState();
        state.setRightAltPressed(true);

        BrowserTextInput input = Objects.requireNonNull(state.committedText(
                "@", Set.of(BrowserModifier.ALT, BrowserModifier.CONTROL, BrowserModifier.SHIFT), 1_000));

        assertEquals(Set.of(BrowserModifier.SHIFT), input.modifiers());
    }

    @Test
    void tracksPointerDragStateAndResetsOnBlur() {
        GrapheneBrowserInputState state = new GrapheneBrowserInputState();
        state.updatePointerButton(BrowserPointerButton.LEFT, true);

        assertEquals(BrowserPointerAction.DRAG, state.pointerMovementAction());
        assertEquals(BrowserPointerButton.LEFT, state.pressedButton());

        state.reset();

        assertEquals(BrowserPointerAction.MOVE, state.pointerMovementAction());
        assertEquals(BrowserPointerButton.NONE, state.pressedButton());
    }
}
