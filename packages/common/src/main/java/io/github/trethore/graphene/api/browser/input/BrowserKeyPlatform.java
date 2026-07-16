package io.github.trethore.graphene.api.browser.input;

import java.util.Locale;

/** Identifies the platform namespace used by raw keyboard metadata. */
public enum BrowserKeyPlatform {
  WINDOWS,
  MACOS,
  LINUX,
  OTHER;

  private static final BrowserKeyPlatform CURRENT = detectCurrent();

  public static BrowserKeyPlatform current() {
    return CURRENT;
  }

  private static BrowserKeyPlatform detectCurrent() {
    return fromOsName(System.getProperty("os.name", ""));
  }

  static BrowserKeyPlatform fromOsName(String osName) {
    String normalizedName = osName.toLowerCase(Locale.ROOT);
    if (normalizedName.contains("mac") || normalizedName.contains("darwin")) {
      return MACOS;
    }
    if (normalizedName.contains("win")) {
      return WINDOWS;
    }
    if (normalizedName.contains("linux")) {
      return LINUX;
    }
    return OTHER;
  }
}
