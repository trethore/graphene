package io.github.trethore.graphene.api.browser.input;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A physical key transition. Text produced by a key is sent separately as {@link BrowserTextInput}.
 */
public record BrowserKeyInput(
    BrowserKeyAction action,
    BrowserKey key,
    BrowserKeyLocation location,
    Set<BrowserModifier> modifiers,
    Optional<BrowserRawKeyMetadata> rawMetadata) {
  public BrowserKeyInput {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(location, "location");
    modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
    Objects.requireNonNull(rawMetadata, "rawMetadata");
  }

  public BrowserKeyInput(
      BrowserKeyAction action,
      BrowserKey key,
      BrowserKeyLocation location,
      Set<BrowserModifier> modifiers) {
    this(action, key, location, modifiers, Optional.empty());
  }

  public BrowserKeyInput(
      BrowserKeyAction action,
      BrowserKey key,
      BrowserKeyLocation location,
      Set<BrowserModifier> modifiers,
      BrowserRawKeyMetadata rawMetadata) {
    this(
        action,
        key,
        location,
        modifiers,
        Optional.of(Objects.requireNonNull(rawMetadata, "rawMetadata")));
  }
}
