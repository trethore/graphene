package io.github.trethore.graphene.api.browser.input;

import java.util.Objects;

/**
 * Optional platform data used to preserve unknown keys and host keyboard-layout behavior. Consumers
 * should not interpret these values as stable application key identifiers.
 */
public record BrowserRawKeyMetadata(
    BrowserKeyPlatform platform, long scanCode, int layoutCodePoint) {
  public BrowserRawKeyMetadata {
    Objects.requireNonNull(platform, "platform");
    if (scanCode < 0) {
      throw new IllegalArgumentException("scanCode must not be negative");
    }
    if (layoutCodePoint != 0
        && (!Character.isValidCodePoint(layoutCodePoint)
            || (layoutCodePoint >= Character.MIN_SURROGATE
                && layoutCodePoint <= Character.MAX_SURROGATE))) {
      throw new IllegalArgumentException(
          "layoutCodePoint must be zero or a valid Unicode code point");
    }
  }
}
