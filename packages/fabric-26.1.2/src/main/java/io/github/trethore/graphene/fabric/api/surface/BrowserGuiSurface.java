package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Projects a browser view through a custom Minecraft GUI render path. */
@SuppressWarnings("unused")
public final class BrowserGuiSurface extends AbstractBrowserGuiSurface {
    private BrowserGuiSurface(Builder builder) {
        super(builder);
    }

    public static Builder builder(BrowserView view) {
        return new Builder(view);
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y) {
        render((GrapheneGuiGraphics) graphics, x, y);
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int renderedWidth, int renderedHeight) {
        render((GrapheneGuiGraphics) graphics, x, y, renderedWidth, renderedHeight);
    }

    public static final class Builder extends AbstractBrowserGuiSurface.Builder {
        private Builder(BrowserView view) {
            super(view);
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

        public BrowserGuiSurface build() {
            return new BrowserGuiSurface(this);
        }
    }
}
