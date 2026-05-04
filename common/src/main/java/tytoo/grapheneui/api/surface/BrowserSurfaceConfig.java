package tytoo.grapheneui.api.surface;

import org.cef.CefBrowserSettings;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Configuration class for browser surface settings, including frame rate and custom browser settings.
 * Provides a builder for easy configuration and immutability.
 */
public final class BrowserSurfaceConfig {
    private static final int DEFAULT_MAX_FPS = 60;
    private static final Consumer<CefBrowserSettings> NO_OP_SETTINGS_CUSTOMIZER = ignoredSettings -> {
    };
    private static final BrowserSurfaceConfig DEFAULT = new Builder().build();
    private static final String SETTINGS_CUSTOMIZER = "settingsCustomizer";

    private final Integer windowlessFrameRate;
    private final boolean windowlessFrameRateExplicit;
    private final Consumer<CefBrowserSettings> settingsCustomizer;

    private BrowserSurfaceConfig(Builder builder) {
        this.windowlessFrameRate = builder.windowlessFrameRate;
        this.windowlessFrameRateExplicit = builder.windowlessFrameRateExplicit;
        this.settingsCustomizer = Objects.requireNonNullElse(builder.settingsCustomizer, NO_OP_SETTINGS_CUSTOMIZER);
    }

    private BrowserSurfaceConfig(
            Integer windowlessFrameRate,
            boolean windowlessFrameRateExplicit,
            Consumer<CefBrowserSettings> settingsCustomizer
    ) {
        this.windowlessFrameRate = windowlessFrameRate;
        this.windowlessFrameRateExplicit = windowlessFrameRateExplicit;
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
        int mergedFrameRate = windowlessFrameRateExplicit
                ? Math.max(windowlessFrameRate, maxFps)
                : maxFps;
        return new BrowserSurfaceConfig(mergedFrameRate, true, settingsCustomizer);
    }

    public BrowserSurfaceConfig withSettingsCustomizer(Consumer<CefBrowserSettings> settingsCustomizer) {
        Consumer<CefBrowserSettings> nonNullCustomizer = Objects.requireNonNull(settingsCustomizer, SETTINGS_CUSTOMIZER);
        return new BrowserSurfaceConfig(
                windowlessFrameRate,
                windowlessFrameRateExplicit,
                this.settingsCustomizer.andThen(nonNullCustomizer)
        );
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
        private boolean windowlessFrameRateExplicit;
        private Consumer<CefBrowserSettings> settingsCustomizer = NO_OP_SETTINGS_CUSTOMIZER;

        private Builder() {
        }

        public Builder maxFps(int maxFps) {
            validateFrameRate(maxFps);
            this.windowlessFrameRate = windowlessFrameRateExplicit
                    ? Math.max(this.windowlessFrameRate, maxFps)
                    : maxFps;
            this.windowlessFrameRateExplicit = true;
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
