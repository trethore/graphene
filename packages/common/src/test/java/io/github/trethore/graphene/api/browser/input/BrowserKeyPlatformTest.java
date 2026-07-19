package io.github.trethore.graphene.api.browser.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrowserKeyPlatformTest {
  @Test
  void detectsPlatformNamespacesWithoutConfusingDarwinWithWindows() {
    assertEquals(BrowserKeyPlatform.WINDOWS, BrowserKeyPlatform.fromOsName("Windows 11"));
    assertEquals(BrowserKeyPlatform.MACOS, BrowserKeyPlatform.fromOsName("Mac OS X"));
    assertEquals(BrowserKeyPlatform.MACOS, BrowserKeyPlatform.fromOsName("Darwin"));
    assertEquals(BrowserKeyPlatform.LINUX, BrowserKeyPlatform.fromOsName("Linux"));
    assertEquals(BrowserKeyPlatform.OTHER, BrowserKeyPlatform.fromOsName("FreeBSD"));
  }
}
