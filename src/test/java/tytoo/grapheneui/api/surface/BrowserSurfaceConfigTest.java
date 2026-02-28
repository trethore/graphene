package tytoo.grapheneui.api.surface;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BrowserSurfaceConfigTest {
    @Test
    void defaultsUseLibraryDefaultMaxFps() {
        BrowserSurfaceConfig config = BrowserSurfaceConfig.defaults();

        assertEquals(60, config.toCefBrowserSettings().windowless_frame_rate);
    }

    @Test
    void withMaxFpsKeepsLargestExplicitValue() {
        BrowserSurfaceConfig config = BrowserSurfaceConfig.defaults()
                .withMaxFps(30)
                .withMaxFps(144)
                .withMaxFps(120);

        assertEquals(144, config.toCefBrowserSettings().windowless_frame_rate);
    }

    @Test
    void builderMaxFpsKeepsLargestExplicitValue() {
        BrowserSurfaceConfig config = BrowserSurfaceConfig.builder()
                .maxFps(72)
                .maxFps(165)
                .maxFps(144)
                .build();

        assertEquals(165, config.toCefBrowserSettings().windowless_frame_rate);
    }
}
