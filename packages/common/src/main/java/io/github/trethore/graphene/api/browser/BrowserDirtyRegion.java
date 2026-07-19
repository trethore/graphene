package io.github.trethore.graphene.api.browser;

/** A rectangular region changed since the preceding browser frame. */
public record BrowserDirtyRegion(int x, int y, int width, int height) {
  public BrowserDirtyRegion {
    if (x < 0 || y < 0) {
      throw new IllegalArgumentException("Dirty region position must not be negative");
    }
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Dirty region dimensions must be positive");
    }
  }
}
