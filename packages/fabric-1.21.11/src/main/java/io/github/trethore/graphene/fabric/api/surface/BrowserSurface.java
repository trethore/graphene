package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserGpuRenderer;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;

@SuppressWarnings("unused")
public final class BrowserSurface extends AbstractBrowserSurface {
    private final GrapheneBrowserGpuRenderer renderer = new GrapheneBrowserGpuRenderer();

    private BrowserSurface(Builder builder) {
        super(
                builder.context,
                builder.url,
                builder.options,
                builder.width,
                builder.height,
                builder.autoResolution,
                builder.resolutionWidth,
                builder.resolutionHeight);
    }

    public static Builder builder(GrapheneContext context) {
        return new Builder(context);
    }

    public void render(GuiGraphics graphics, int x, int y) {
        render(graphics, x, y, width(), height());
    }

    @SuppressWarnings("resource")
    public void render(GuiGraphics graphics, int x, int y, int renderedWidth, int renderedHeight) {
        ensureOpen();
        GuiGraphics validatedGraphics = Objects.requireNonNull(graphics, "graphics");
        int validatedWidth = requirePositive(renderedWidth, "renderedWidth");
        int validatedHeight = requirePositive(renderedHeight, "renderedHeight");
        Optional<BrowserFrame> availableFrame = browser().latestFrame();
        if (availableFrame.isEmpty()) {
            return;
        }
        renderer.render(
                validatedGraphics,
                availableFrame.get(),
                browser().options().transparent(),
                x,
                y,
                validatedWidth,
                validatedHeight);
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }
        renderer.close();
        super.close();
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
    }
}
