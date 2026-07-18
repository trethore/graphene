package io.github.trethore.graphene.api.browser.input;

import java.util.Objects;
import java.util.Set;

/**
 * A scroll event expressed in browser viewport coordinates. Deltas use conventional wheel units,
 * where {@code 120} represents one wheel notch.
 */
public record BrowserScrollInput(
    int x, int y, int deltaX, int deltaY, Set<BrowserModifier> modifiers) {
  public BrowserScrollInput {
    modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
  }
}
