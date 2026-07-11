package io.github.trethore.graphene.api.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BrowserOptionsTest {
  @Test
  void validatesMaximumFrameRate() {
    BrowserOptions.Builder minimumBuilder = BrowserOptions.builder();
    BrowserOptions.Builder maximumBuilder = BrowserOptions.builder();
    assertThrows(IllegalArgumentException.class, () -> minimumBuilder.maximumFrameRate(0));
    assertThrows(IllegalArgumentException.class, () -> maximumBuilder.maximumFrameRate(61));
    assertEquals(30, BrowserOptions.builder().maximumFrameRate(30).build().maximumFrameRate());
  }
}
