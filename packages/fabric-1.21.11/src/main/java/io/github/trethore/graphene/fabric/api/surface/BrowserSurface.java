package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;

@SuppressWarnings("unused")
public final class BrowserSurface extends AbstractBrowserSurface {
    private BrowserSurface(Builder builder) {
        super(builder);
    }

    public static Builder builder(GrapheneContext context) {
        return new Builder(context);
    }

    public void render(GuiGraphics graphics, int x, int y) {
        render((GrapheneGuiGraphics) graphics, x, y);
    }

    public void render(GuiGraphics graphics, int x, int y, int renderedWidth, int renderedHeight) {
        render((GrapheneGuiGraphics) graphics, x, y, renderedWidth, renderedHeight);
    }

    public static final class Builder extends AbstractBrowserSurface.Builder {
        private Builder(GrapheneContext context) {
            super(context);
        }

        public Builder url(String url) {
            setUrl(url);
            return this;
        }

        public Builder options(BrowserOptions options) {
            setOptions(options);
            return this;
        }

        public Builder size(int width, int height) {
            setSize(width, height);
            return this;
        }

        public Builder resolution(int width, int height) {
            setResolution(width, height);
            return this;
        }

        public Builder autoResolution() {
            enableAutoResolution();
            return this;
        }

        public BrowserSurface build() {
            return new BrowserSurface(this);
        }
    }
}
