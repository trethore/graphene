package io.github.trethore.graphene.api.browser.input;

import java.util.Objects;
import java.util.Set;

/**
 * Committed Unicode text input. This value does not represent input-method composition or preedit
 * state.
 */
public record BrowserTextInput(String text, Set<BrowserModifier> modifiers) {
  public BrowserTextInput {
    Objects.requireNonNull(text, "text");
    if (text.isEmpty()) {
      throw new IllegalArgumentException("text must not be empty");
    }
    validateUnicode(text);
    modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
  }

  private static void validateUnicode(String text) {
    int index = 0;
    while (index < text.length()) {
      char character = text.charAt(index);
      if (Character.isHighSurrogate(character)) {
        if (index + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(index + 1))) {
          throw new IllegalArgumentException("text must contain well-formed UTF-16");
        }
        index += 2;
      } else if (Character.isLowSurrogate(character)) {
        throw new IllegalArgumentException("text must contain well-formed UTF-16");
      } else {
        index++;
      }
    }
  }
}
