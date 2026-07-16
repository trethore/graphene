package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserGpuRenderer;
import io.github.trethore.graphene.fabric.internal.util.MinecraftReferences;
import io.github.trethore.graphene.internal.browser.GrapheneSurfaceSizingState;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;

@SuppressWarnings("unused")
public final class BrowserSurface implements AutoCloseable {
  private static final String WIDTH_NAME = "width";
  private static final String HEIGHT_NAME = "height";

  private final BrowserSession browser;
  private final GrapheneBrowserGpuRenderer renderer = new GrapheneBrowserGpuRenderer();
  private final GrapheneSurfaceSizingState sizing;
  private boolean closed;

  private BrowserSurface(Builder builder) {
    sizing =
        new GrapheneSurfaceSizingState(
            builder.width,
            builder.height,
            builder.autoResolution,
            builder.resolutionWidth,
            builder.resolutionHeight,
            scaleFactor());
    browser =
        builder
            .context
            .browsers()
            .create(
                builder.url, builder.options, sizing.resolutionWidth(), sizing.resolutionHeight());
  }

  public static Builder builder(GrapheneContext context) {
    return new Builder(context);
  }

  public BrowserSession browser() {
    return browser;
  }

  public int width() {
    return sizing.width();
  }

  public int height() {
    return sizing.height();
  }

  public int resolutionWidth() {
    return sizing.resolutionWidth();
  }

  public int resolutionHeight() {
    return sizing.resolutionHeight();
  }

  public boolean isAutoResolution() {
    return sizing.autoResolution();
  }

  public void render(GuiGraphics graphics, int x, int y) {
    render(graphics, x, y, sizing.width(), sizing.height());
  }

  public void render(GuiGraphics graphics, int x, int y, int renderedWidth, int renderedHeight) {
    ensureOpen();
    GuiGraphics validatedGraphics = Objects.requireNonNull(graphics, "graphics");
    int validatedWidth = requirePositive(renderedWidth, "renderedWidth");
    int validatedHeight = requirePositive(renderedHeight, "renderedHeight");
    Optional<BrowserFrame> availableFrame = browser.latestFrame();
    if (availableFrame.isEmpty()) {
      return;
    }
    renderer.render(
        validatedGraphics,
        availableFrame.get(),
        browser.options().transparent(),
        x,
        y,
        validatedWidth,
        validatedHeight);
  }

  public void resize(int width, int height) {
    ensureOpen();
    applyResize(sizing.resize(width, height, scaleFactor()));
  }

  public void setResolution(int width, int height) {
    ensureOpen();
    applyResize(sizing.setResolution(width, height));
  }

  public void useAutoResolution() {
    ensureOpen();
    applyResize(sizing.useAutoResolution(scaleFactor()));
  }

  public int toBrowserX(double surfaceX, int renderedWidth) {
    return sizing.mapX(surfaceX, renderedWidth);
  }

  public int toBrowserY(double surfaceY, int renderedHeight) {
    return sizing.mapY(surfaceY, renderedHeight);
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

  private void applyResize(GrapheneSurfaceSizingState.Resize resize) {
    if (resize.required()) {
      browser.resize(resize.width(), resize.height());
    }
  }

  private static double scaleFactor() {
    return MinecraftReferences.guiScale();
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
