package io.github.trethore.graphene.api.browser;

import java.util.Objects;

public record BrowserLoadFailed(
    int browserId, String url, boolean mainFrame, int errorCode, String errorName, String message) {
  public BrowserLoadFailed {
    url = Objects.requireNonNullElse(url, "");
    errorName = Objects.requireNonNullElse(errorName, "UNKNOWN");
    message = Objects.requireNonNullElse(message, "");
  }
}
