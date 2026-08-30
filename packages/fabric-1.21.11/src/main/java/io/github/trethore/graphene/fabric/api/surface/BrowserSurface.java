package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Compatibility facade that owns a {@link BrowserView} and projects it into GUI space.
 *
 * @deprecated Use {@link BrowserView} with {@link BrowserGuiSurface}.
 */
@Deprecated(since = "2.3.0")
@SuppressWarnings("unused")
public final class BrowserSurface implements AutoCloseable {
    private final BrowserView view;
    private final BrowserGuiSurface guiSurface;

    private BrowserSurface(Builder builder) {
        view = BrowserView.builder(builder.context)
                .url(builder.url)
                .options(builder.options)
                .build();
        BrowserGuiSurface.Builder surfaceBuilder =
                BrowserGuiSurface.builder(view).size(builder.width, builder.height);
        if (builder.autoResolution) {
            surfaceBuilder.autoResolution();
        } else {
            surfaceBuilder.resolution(builder.resolutionWidth, builder.resolutionHeight);
        }
        guiSurface = surfaceBuilder.build();
    }

    public static Builder builder(GrapheneContext context) {
        return new Builder(context);
    }

    public BrowserView view() {
        return view;
    }

    public BrowserGuiSurface guiSurface() {
        return guiSurface;
    }

    public BrowserSession browser() {
        return view.browser();
    }

    public int width() {
        return guiSurface.width();
    }

    public int height() {
        return guiSurface.height();
    }

    public int resolutionWidth() {
        return view.resolutionWidth();
    }

    public int resolutionHeight() {
        return view.resolutionHeight();
    }

    public boolean isAutoResolution() {
        return guiSurface.isAutoResolution();
    }

    public void resize(int width, int height) {
        guiSurface.resize(width, height);
    }

    public void setResolution(int width, int height) {
        guiSurface.setResolution(width, height);
    }

    public void useAutoResolution() {
        guiSurface.useAutoResolution();
    }

    public int toBrowserX(double surfaceX, int renderedWidth) {
        return guiSurface.toBrowserX(surfaceX, renderedWidth);
    }

    public int toBrowserY(double surfaceY, int renderedHeight) {
        return guiSurface.toBrowserY(surfaceY, renderedHeight);
    }

    public void render(GuiGraphics graphics, int x, int y) {
        guiSurface.render(graphics, x, y);
    }

    public void render(GuiGraphics graphics, int x, int y, int renderedWidth, int renderedHeight) {
        guiSurface.render(graphics, x, y, renderedWidth, renderedHeight);
    }

    @Override
    public void close() {
        view.close();
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
            this.width = requirePositive(width, "width");
            this.height = requirePositive(height, "height");
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

        private static int requirePositive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}
