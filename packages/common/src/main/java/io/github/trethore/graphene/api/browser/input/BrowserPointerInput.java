package io.github.trethore.graphene.api.browser.input;

import java.util.Objects;
import java.util.Set;

/**
 * A pointer event expressed in browser viewport coordinates. The click count is zero for movement
 * and otherwise identifies the current repeated-click sequence.
 */
public record BrowserPointerInput(
    BrowserPointerAction action,
    int x,
    int y,
    BrowserPointerButton button,
    int clickCount,
    Set<BrowserModifier> modifiers) {
  public BrowserPointerInput {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(button, "button");
    modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
    if (clickCount < 0) {
      throw new IllegalArgumentException("clickCount must not be negative");
    }
  }
}
