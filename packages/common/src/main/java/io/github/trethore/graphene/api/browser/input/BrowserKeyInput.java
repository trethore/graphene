package io.github.trethore.graphene.api.browser.input;

import java.util.Objects;
import java.util.Set;

public record BrowserKeyInput(
    BrowserKeyAction action,
    int keyCode,
    int nativeKeyCode,
    long scanCode,
    boolean systemKey,
    Set<BrowserModifier> modifiers) {
  public BrowserKeyInput {
    Objects.requireNonNull(action, "action");
    modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
  }
}
