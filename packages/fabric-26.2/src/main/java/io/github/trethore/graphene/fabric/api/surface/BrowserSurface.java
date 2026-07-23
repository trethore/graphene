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
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A Minecraft render surface that owns and displays one browser session. Logical size controls
 * layout and input mapping, while browser resolution controls the pixel dimensions rendered by the
 * session; automatic resolution derives those pixels from Minecraft's GUI scale.
 */
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

  /** Returns the browser session owned by this surface. */
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

  /** Renders the latest available browser frame at the surface's logical size. */
  public void render(GuiGraphicsExtractor graphics, int x, int y) {
    render(graphics, x, y, sizing.width(), sizing.height());
  }

  /** Renders the latest available browser frame at an explicit display size. */
  public void render(
      GuiGraphicsExtractor graphics, int x, int y, int renderedWidth, int renderedHeight) {
    ensureOpen();
    GuiGraphicsExtractor validatedGraphics = Objects.requireNonNull(graphics, "graphics");
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

  /**
   * Changes the logical size and updates browser resolution when automatic resolution is active.
   */
  public void resize(int width, int height) {
    ensureOpen();
    applyResize(sizing.resize(width, height, scaleFactor()));
  }

  /** Selects a fixed browser pixel resolution and disables automatic resolution. */
  public void setResolution(int width, int height) {
    ensureOpen();
    applyResize(sizing.setResolution(width, height));
  }

  /** Enables browser resolution derived from logical size and the current GUI scale. */
  public void useAutoResolution() {
    ensureOpen();
    applyResize(sizing.useAutoResolution(scaleFactor()));
  }

  /** Maps a horizontal coordinate within the rendered surface to browser pixel coordinates. */
  public int toBrowserX(double surfaceX, int renderedWidth) {
    return sizing.mapX(surfaceX, renderedWidth);
  }

  /** Maps a vertical coordinate within the rendered surface to browser pixel coordinates. */
  public int toBrowserY(double surfaceY, int renderedHeight) {
    return sizing.mapY(surfaceY, renderedHeight);
  }

  /** Closes the renderer and the browser session owned by this surface. */
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

  /** Builds a surface and its owned browser session. */
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

    /** Sets the logical surface size. */
    public Builder size(int width, int height) {
      this.width = requirePositive(width, WIDTH_NAME);
      this.height = requirePositive(height, HEIGHT_NAME);
      return this;
    }

    /** Sets a fixed browser pixel resolution and disables automatic resolution. */
    public Builder resolution(int width, int height) {
      autoResolution = false;
      resolutionWidth = requirePositive(width, "resolutionWidth");
      resolutionHeight = requirePositive(height, "resolutionHeight");
      return this;
    }

    /** Uses browser resolution derived from logical size and Minecraft GUI scale. */
    public Builder autoResolution() {
      autoResolution = true;
      return this;
    }

    public BrowserSurface build() {
      return new BrowserSurface(this);
    }
  }
}
