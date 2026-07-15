package io.github.trethore.graphene.api.browser.download;

/** Process-unique Graphene download identifier. */
public record BrowserDownloadId(long value) {
  public BrowserDownloadId {
    if (value < 0) {
      throw new IllegalArgumentException("value must not be negative");
    }
  }
}
