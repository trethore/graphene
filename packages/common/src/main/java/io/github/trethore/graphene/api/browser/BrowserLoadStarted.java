package io.github.trethore.graphene.api.browser;

import java.util.Objects;

public record BrowserLoadStarted(
    int browserId, String url, boolean mainFrame, String navigationType) {
  public BrowserLoadStarted {
    url = Objects.requireNonNullElse(url, "");
    navigationType = Objects.requireNonNullElse(navigationType, "UNKNOWN");
  }
}
