package io.github.trethore.graphene.api.browser;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A frame load that completed without a navigation-level failure. HTTP error responses may still
 * complete successfully. The HTTP status is empty for non-HTTP loads or when unavailable.
 */
public record BrowserLoadCompleted(String url, boolean mainFrame, OptionalInt httpStatus) {
  public BrowserLoadCompleted {
    url = Objects.requireNonNullElse(url, "");
    Objects.requireNonNull(httpStatus, "httpStatus");
  }
}
