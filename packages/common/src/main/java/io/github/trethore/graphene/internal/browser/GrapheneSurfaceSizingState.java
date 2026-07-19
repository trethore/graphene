package io.github.trethore.graphene.internal.browser;

public final class GrapheneSurfaceSizingState {
  private int width;
  private int height;
  private int resolutionWidth;
  private int resolutionHeight;
  private boolean autoResolution;

  public GrapheneSurfaceSizingState(
      int width,
      int height,
      boolean autoResolution,
      int resolutionWidth,
      int resolutionHeight,
      double scaleFactor) {
    this.width = requirePositive(width, "width");
    this.height = requirePositive(height, "height");
    this.autoResolution = autoResolution;
    if (autoResolution) {
      updateAutoResolution(scaleFactor);
    } else {
      this.resolutionWidth = requirePositive(resolutionWidth, "resolutionWidth");
      this.resolutionHeight = requirePositive(resolutionHeight, "resolutionHeight");
    }
  }

  public Resize resize(int width, int height, double scaleFactor) {
    this.width = requirePositive(width, "width");
    this.height = requirePositive(height, "height");
    if (!autoResolution) {
      return Resize.none();
    }
    updateAutoResolution(scaleFactor);
    return Resize.to(resolutionWidth, resolutionHeight);
  }

  public Resize setResolution(int width, int height) {
    autoResolution = false;
    resolutionWidth = requirePositive(width, "resolutionWidth");
    resolutionHeight = requirePositive(height, "resolutionHeight");
    return Resize.to(resolutionWidth, resolutionHeight);
  }

  public Resize useAutoResolution(double scaleFactor) {
    autoResolution = true;
    updateAutoResolution(scaleFactor);
    return Resize.to(resolutionWidth, resolutionHeight);
  }

  public int mapX(double coordinate, int renderedWidth) {
    return mapCoordinate(coordinate, renderedWidth, resolutionWidth);
  }

  public int mapY(double coordinate, int renderedHeight) {
    return mapCoordinate(coordinate, renderedHeight, resolutionHeight);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public int resolutionWidth() {
    return resolutionWidth;
  }

  public int resolutionHeight() {
    return resolutionHeight;
  }

  public boolean autoResolution() {
    return autoResolution;
  }

  static int mapCoordinate(double coordinate, int renderedSize, int sourceSize) {
    int validatedRenderedSize = requirePositive(renderedSize, "renderedSize");
    int validatedSourceSize = requirePositive(sourceSize, "sourceSize");
    double normalized = Math.clamp(coordinate / validatedRenderedSize, 0.0, 1.0);
    return Math.min((int) Math.floor(normalized * validatedSourceSize), validatedSourceSize - 1);
  }

  private void updateAutoResolution(double scaleFactor) {
    if (!Double.isFinite(scaleFactor) || scaleFactor <= 0.0) {
      throw new IllegalArgumentException("scaleFactor must be finite and positive");
    }
    resolutionWidth = Math.max(1, (int) Math.round(width * scaleFactor));
    resolutionHeight = Math.max(1, (int) Math.round(height * scaleFactor));
  }

  private static int requirePositive(int value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  public record Resize(boolean required, int width, int height) {
    private static Resize none() {
      return new Resize(false, 1, 1);
    }

    private static Resize to(int width, int height) {
      return new Resize(true, width, height);
    }
  }
}
