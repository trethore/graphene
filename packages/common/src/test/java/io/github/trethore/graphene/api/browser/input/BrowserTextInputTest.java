package io.github.trethore.graphene.api.browser.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BrowserTextInputTest {
  @Test
  void acceptsSupplementaryUnicodeAndCombiningSequences() {
    BrowserTextInput input = new BrowserTextInput("\uD83D\uDE00e\u0301", Set.of());

    assertEquals("\uD83D\uDE00e\u0301", input.text());
  }

  @Test
  void rejectsEmptyAndMalformedText() {
    assertThrows(IllegalArgumentException.class, () -> new BrowserTextInput("", Set.of()));
    assertThrows(IllegalArgumentException.class, () -> new BrowserTextInput("\uD83D", Set.of()));
    assertThrows(IllegalArgumentException.class, () -> new BrowserTextInput("\uDE00", Set.of()));
  }
}
