package io.github.trethore.graphene.api.browser.input;

import java.util.Objects;
import java.util.Set;

public record BrowserTextInput(char character, Set<BrowserModifier> modifiers) {
  public BrowserTextInput {
    modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
  }
}
