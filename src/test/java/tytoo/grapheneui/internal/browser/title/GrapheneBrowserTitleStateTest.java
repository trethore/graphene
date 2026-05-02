package tytoo.grapheneui.internal.browser.title;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneBrowserTitleStateTest {
    @Test
    void startsWithEmptyTitle() {
        GrapheneBrowserTitleState titleState = new GrapheneBrowserTitleState();

        assertEquals("", titleState.currentTitle());
    }

    @Test
    void updateTitleReturnsWhetherTitleChanged() {
        GrapheneBrowserTitleState titleState = new GrapheneBrowserTitleState();

        assertTrue(titleState.updateTitle("Graphene"));
        assertEquals("Graphene", titleState.currentTitle());
        assertFalse(titleState.updateTitle("Graphene"));
    }

    @Test
    void nullTitleIsNormalizedToEmptyString() {
        GrapheneBrowserTitleState titleState = new GrapheneBrowserTitleState();

        titleState.updateTitle("Graphene");

        assertTrue(titleState.updateTitle(null));
        assertEquals("", titleState.currentTitle());
    }
}
