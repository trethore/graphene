package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserGpuRenderer;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

@SuppressWarnings("unused")
public final class BrowserSurface implements AutoCloseable {
  private static final String WIDTH_NAME = "width";
  private static final String HEIGHT_NAME = "height";

  private final BrowserSession browser;
  private final GrapheneBrowserGpuRenderer renderer = new GrapheneBrowserGpuRenderer();
  private int width;
  private int height;
  private int resolutionWidth;
  private int resolutionHeight;
  private boolean autoResolution;
  private boolean closed;

  private BrowserSurface(Builder builder) {
    width = builder.width;
    height = builder.height;
    autoResolution = builder.autoResolution;
    resolutionWidth = autoResolution ? scaledResolution(builder.width) : builder.resolutionWidth;
    resolutionHeight = autoResolution ? scaledResolution(builder.height) : builder.resolutionHeight;
    browser =
        builder
            .context
            .browsers()
            .create(builder.url, builder.options, resolutionWidth, resolutionHeight);
  }

  public static Builder builder(GrapheneContext context) {
    return new Builder(context);
  }

  public BrowserSession browser() {
    return browser;
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

  public boolean isAutoResolution() {
    return autoResolution;
  }

  public void render(GuiGraphics graphics, int x, int y) {
    render(graphics, x, y, width, height);
  }

  public void render(GuiGraphics graphics, int x, int y, int renderedWidth, int renderedHeight) {
    ensureOpen();
    renderer.render(
        Objects.requireNonNull(graphics, "graphics"),
        browser.latestFrame(),
        browser.options().transparent(),
        x,
        y,
        requirePositive(renderedWidth, "renderedWidth"),
        requirePositive(renderedHeight, "renderedHeight"));
  }

  public void resize(int width, int height) {
    ensureOpen();
    this.width = requirePositive(width, WIDTH_NAME);
    this.height = requirePositive(height, HEIGHT_NAME);
    if (autoResolution) {
      setBrowserResolution(scaledResolution(width), scaledResolution(height));
    }
  }

  public void setResolution(int width, int height) {
    ensureOpen();
    autoResolution = false;
    setBrowserResolution(requirePositive(width, WIDTH_NAME), requirePositive(height, HEIGHT_NAME));
  }

  public void useAutoResolution() {
    ensureOpen();
    autoResolution = true;
    setBrowserResolution(scaledResolution(width), scaledResolution(height));
  }

  public int toBrowserX(double surfaceX, int renderedWidth) {
    return mapCoordinate(surfaceX, renderedWidth, resolutionWidth);
  }

  public int toBrowserY(double surfaceY, int renderedHeight) {
    return mapCoordinate(surfaceY, renderedHeight, resolutionHeight);
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    renderer.close();
    browser.close();
  }

  private void setBrowserResolution(int width, int height) {
    resolutionWidth = width;
    resolutionHeight = height;
    browser.resize(width, height);
  }

  private static int mapCoordinate(double coordinate, int renderedSize, int resolutionSize) {
    int validatedRenderedSize = requirePositive(renderedSize, "renderedSize");
    double normalized = Math.clamp(coordinate / validatedRenderedSize, 0.0D, 1.0D);
    return Math.min((int) Math.floor(normalized * resolutionSize), resolutionSize - 1);
  }

  private static int scaledResolution(int logicalSize) {
    double scale = Minecraft.getInstance().getWindow().getGuiScale();
    return Math.max(1, (int) Math.round(logicalSize * scale));
  }

  private static int requirePositive(int value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("BrowserSurface is closed");
    }
  }

  public static final class Builder {
    private final GrapheneContext context;
    private String url = "about:blank";
    private BrowserOptions options = BrowserOptions.defaults();
    private int width = 1;
    private int height = 1;
    private boolean autoResolution = true;
    private int resolutionWidth = 1;
    private int resolutionHeight = 1;

    private Builder(GrapheneContext context) {
      this.context = Objects.requireNonNull(context, "context");
    }

    public Builder url(String url) {
      this.url = Objects.requireNonNull(url, "url");
      return this;
    }

    public Builder options(BrowserOptions options) {
      this.options = Objects.requireNonNull(options, "options");
      return this;
    }

    public Builder size(int width, int height) {
      this.width = requirePositive(width, WIDTH_NAME);
      this.height = requirePositive(height, HEIGHT_NAME);
      return this;
    }

    public Builder resolution(int width, int height) {
      autoResolution = false;
      resolutionWidth = requirePositive(width, "resolutionWidth");
      resolutionHeight = requirePositive(height, "resolutionHeight");
      return this;
    }

    public Builder autoResolution() {
      autoResolution = true;
      return this;
    }

    public BrowserSurface build() {
      return new BrowserSurface(this);
    }
  }
}
