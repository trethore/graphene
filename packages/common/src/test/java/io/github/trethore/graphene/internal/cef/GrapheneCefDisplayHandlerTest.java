package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.browser.BrowserConsoleSeverity;
import org.cef.CefSettings;
import org.junit.jupiter.api.Test;

final class GrapheneCefDisplayHandlerTest {
  @Test
  void mapsCefConsoleSeveritiesToPublicValues() {
    assertEquals(BrowserConsoleSeverity.UNKNOWN, GrapheneCefDisplayHandler.severity(null));
    assertEquals(
        BrowserConsoleSeverity.INFO,
        GrapheneCefDisplayHandler.severity(CefSettings.LogSeverity.LOGSEVERITY_DEFAULT));
    assertEquals(
        BrowserConsoleSeverity.VERBOSE,
        GrapheneCefDisplayHandler.severity(CefSettings.LogSeverity.LOGSEVERITY_VERBOSE));
    assertEquals(
        BrowserConsoleSeverity.INFO,
        GrapheneCefDisplayHandler.severity(CefSettings.LogSeverity.LOGSEVERITY_INFO));
    assertEquals(
        BrowserConsoleSeverity.WARNING,
        GrapheneCefDisplayHandler.severity(CefSettings.LogSeverity.LOGSEVERITY_WARNING));
    assertEquals(
        BrowserConsoleSeverity.ERROR,
        GrapheneCefDisplayHandler.severity(CefSettings.LogSeverity.LOGSEVERITY_ERROR));
    assertEquals(
        BrowserConsoleSeverity.FATAL,
        GrapheneCefDisplayHandler.severity(CefSettings.LogSeverity.LOGSEVERITY_FATAL));
    assertEquals(
        BrowserConsoleSeverity.UNKNOWN,
        GrapheneCefDisplayHandler.severity(CefSettings.LogSeverity.LOGSEVERITY_DISABLE));
  }
}
