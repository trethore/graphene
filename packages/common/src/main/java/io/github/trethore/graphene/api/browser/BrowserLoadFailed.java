package io.github.trethore.graphene.api.browser;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A navigation-level frame load failure. The optional diagnostic code is backend-specific and must
 * only be used for diagnostics, not application control flow.
 */
public record BrowserLoadFailed(
    String url,
    boolean mainFrame,
    BrowserLoadFailureReason reason,
    String message,
    OptionalInt diagnosticCode) {
  public BrowserLoadFailed {
    url = Objects.requireNonNullElse(url, "");
    reason = Objects.requireNonNullElse(reason, BrowserLoadFailureReason.UNKNOWN);
    message = Objects.requireNonNullElse(message, "");
    Objects.requireNonNull(diagnosticCode, "diagnosticCode");
  }
}
