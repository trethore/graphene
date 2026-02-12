package tytoo.grapheneui.browser;

import org.cef.CefBrowserSettings;

import java.util.Objects;
import java.util.function.Consumer;

public final class BrowserSurfaceConfig {
    private static final int DEFAULT_MAX_FPS = 60;
    private static final Consumer<CefBrowserSettings> NO_OP_SETTINGS_CUSTOMIZER = ignoredSettings -> {
    };
    private static final BrowserSurfaceConfig DEFAULT = new Builder().build();
    private static final String SETTINGS_CUSTOMIZER = "settingsCustomizer";

    private final Integer windowlessFrameRate;
    private final Consumer<CefBrowserSettings> settingsCustomizer;

    private BrowserSurfaceConfig(Builder builder) {
        this.windowlessFrameRate = builder.windowlessFrameRate;
        this.settingsCustomizer = Objects.requireNonNullElse(builder.settingsCustomizer, NO_OP_SETTINGS_CUSTOMIZER);
    }

    private BrowserSurfaceConfig(Integer windowlessFrameRate, Consumer<CefBrowserSettings> settingsCustomizer) {
        this.windowlessFrameRate = windowlessFrameRate;
        this.settingsCustomizer = Objects.requireNonNullElse(settingsCustomizer, NO_OP_SETTINGS_CUSTOMIZER);
    }

    public static BrowserSurfaceConfig defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void validateFrameRate(int maxFps) {
        if (maxFps <= 0) {
            throw new IllegalArgumentException("maxFps must be > 0");
        }
    }

    public BrowserSurfaceConfig withMaxFps(int maxFps) {
        validateFrameRate(maxFps);
        return new BrowserSurfaceConfig(maxFps, settingsCustomizer);
    }

    public BrowserSurfaceConfig withSettingsCustomizer(Consumer<CefBrowserSettings> settingsCustomizer) {
        Consumer<CefBrowserSettings> nonNullCustomizer = Objects.requireNonNull(settingsCustomizer, SETTINGS_CUSTOMIZER);
        return new BrowserSurfaceConfig(windowlessFrameRate, this.settingsCustomizer.andThen(nonNullCustomizer));
    }

    public CefBrowserSettings toCefBrowserSettings() {
        CefBrowserSettings cefBrowserSettings = new CefBrowserSettings();
        if (windowlessFrameRate != null) {
            cefBrowserSettings.windowless_frame_rate = windowlessFrameRate;
        }

        settingsCustomizer.accept(cefBrowserSettings);
        return cefBrowserSettings;
    }

    public static final class Builder {
        private int windowlessFrameRate = DEFAULT_MAX_FPS;
        private Consumer<CefBrowserSettings> settingsCustomizer = NO_OP_SETTINGS_CUSTOMIZER;

        private Builder() {
        }

        public Builder maxFps(int maxFps) {
            validateFrameRate(maxFps);
            this.windowlessFrameRate = maxFps;
            return this;
        }

        public Builder settingsCustomizer(Consumer<CefBrowserSettings> settingsCustomizer) {
            this.settingsCustomizer = this.settingsCustomizer.andThen(Objects.requireNonNull(settingsCustomizer, SETTINGS_CUSTOMIZER));
            return this;
        }

        public BrowserSurfaceConfig build() {
            return new BrowserSurfaceConfig(this);
        }
    }
}
