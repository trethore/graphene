package tytoo.grapheneui.internal.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneDebugLogSelectorTest {
    @Test
    void wildcardSelectorMatchesAllPackages() {
        GrapheneDebugLogSelector selector = GrapheneDebugLogSelector.fromRawSelector("*");

        assertTrue(selector.matches("tytoo.grapheneui.internal.cef.GrapheneCefRuntime"));
        assertTrue(selector.matches("tytoo.grapheneuidebug.screen.GrapheneBrowserDebugScreen"));
        assertTrue(selector.matches("any.other.package.ClassName"));
    }

    @Test
    void singlePrefixMatchesExactAndNestedPackages() {
        GrapheneDebugLogSelector selector = GrapheneDebugLogSelector.fromRawSelector("tytoo.grapheneui.internal.cef");

        assertTrue(selector.matches("tytoo.grapheneui.internal.cef"));
        assertTrue(selector.matches("tytoo.grapheneui.internal.cef.GrapheneCefRuntime"));
        assertTrue(selector.matches("tytoo.grapheneui.internal.cef.alert.GrapheneJsDialogManager"));
        assertFalse(selector.matches("tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime"));
    }

    @Test
    void commaSeparatedPrefixesSupportWhitespace() {
        GrapheneDebugLogSelector selector = GrapheneDebugLogSelector.fromRawSelector(
                " tytoo.grapheneui.internal.bridge , tytoo.grapheneuidebug "
        );

        assertTrue(selector.matches("tytoo.grapheneui.internal.bridge.GrapheneBridgeEndpoint"));
        assertTrue(selector.matches("tytoo.grapheneuidebug.test.GrapheneDebugTestRunner"));
        assertFalse(selector.matches("tytoo.grapheneui.internal.cef.GrapheneCefRuntime"));
    }

    @Test
    void blankTokensAndTrailingDotsAreIgnored() {
        GrapheneDebugLogSelector selector = GrapheneDebugLogSelector.fromRawSelector(
                " , tytoo.grapheneui.internal.cef. , ,"
        );

        assertTrue(selector.matches("tytoo.grapheneui.internal.cef.GrapheneCefInstaller"));
        assertFalse(selector.matches("tytoo.grapheneui.internal.event.GrapheneLoadEventBus"));
    }

    @Test
    void blankOrNullSelectorDisablesDebugLogging() {
        GrapheneDebugLogSelector blankSelector = GrapheneDebugLogSelector.fromRawSelector("   ");
        GrapheneDebugLogSelector nullSelector = GrapheneDebugLogSelector.fromRawSelector(null);

        assertFalse(blankSelector.matches("tytoo.grapheneui.internal.cef.GrapheneCefRuntime"));
        assertFalse(nullSelector.matches("tytoo.grapheneui.internal.cef.GrapheneCefRuntime"));
    }
}
