package io.github.trethore.graphene.api.browser;

import java.util.Objects;

public record BrowserConsoleMessage(
    BrowserConsoleSeverity severity, String message, String source, int line) {
  public BrowserConsoleMessage {
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(source, "source");
  }
}
