package io.github.trethore.graphene.api.browser;

import java.util.Objects;

public record BrowserLoadCompleted(int browserId, String url, boolean mainFrame, int httpStatus) {
  public BrowserLoadCompleted {
    url = Objects.requireNonNullElse(url, "");
  }
}
