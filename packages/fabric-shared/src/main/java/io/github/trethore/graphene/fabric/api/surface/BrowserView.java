package io.github.trethore.graphene.fabric.api.surface;

import io.github.trethore.graphene.api.ExperimentalGrapheneApi;
import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserFrameTexture;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserGpuTexture;
import io.github.trethore.graphene.internal.browser.GrapheneBrowserViewState;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns an off-screen browser, its pixel resolution, and its uploaded GPU frame independently of any
 * GUI or world-space projection.
 */
@SuppressWarnings("unused")
public final class BrowserView implements AutoCloseable {
    private final GrapheneBrowserViewState state;
    private final GrapheneBrowserFrameTexture frameTexture;
    private final BrowserTexture texture;

    private BrowserView(Builder builder) {
        state = new GrapheneBrowserViewState(
                builder.context, builder.url, builder.options, builder.resolutionWidth, builder.resolutionHeight);
        try {
            frameTexture = new GrapheneBrowserFrameTexture();
        } catch (RuntimeException exception) {
            state.close();
            throw exception;
        }
        texture = new BrowserTexture(frameTexture.texture());
    }

    public static Builder builder(GrapheneContext context) {
        return new Builder(context);
    }

    public BrowserSession browser() {
        return state.browser();
    }

    public int resolutionWidth() {
        return state.resolutionWidth();
    }

    public int resolutionHeight() {
        return state.resolutionHeight();
    }

    public void setResolution(int width, int height) {
        state.setResolution(width, height);
    }

    public int toBrowserX(double normalizedX) {
        state.ensureOpen();
        return state.mapX(normalizedX);
    }

    public int toBrowserY(double normalizedY) {
        state.ensureOpen();
        return state.mapY(normalizedY);
    }

    /**
     * Uploads the latest browser frame, if available, and returns borrowed access to its GPU
     * texture.
     */
    @ExperimentalGrapheneApi
    public Optional<BrowserTexture> texture() {
        return prepareTexture().map(ignoredTexture -> texture);
    }

    @Override
    public void close() {
        if (state.isClosed()) {
            return;
        }
        frameTexture.close();
        state.close();
    }

    @SuppressWarnings("resource")
    Optional<GrapheneBrowserGpuTexture> prepareTexture() {
        state.ensureOpen();
        return browser()
                .latestFrame()
                .map(frame -> frameTexture.update(frame, browser().options().transparent()));
    }

    /** Builds a browser view. */
    public static final class Builder {
        private final GrapheneContext context;
        private String url = "about:blank";
        private BrowserOptions options = BrowserOptions.defaults();
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

        public Builder resolution(int width, int height) {
            resolutionWidth = requirePositive(width, "width");
            resolutionHeight = requirePositive(height, "height");
            return this;
        }

        public BrowserView build() {
            return new BrowserView(this);
        }

        private static int requirePositive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}
