package tytoo.grapheneui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class GrapheneHttpConfigTest {
    @Test
    void defaultsUseLoopbackAndDefaultRange() {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder().build();

        assertEquals("http", config.baseUrlScheme());
        assertEquals("127.0.0.1", config.bindHost());
        assertTrue(config.fixedPort().isEmpty());
        assertTrue(config.randomPortRange().isPresent());
        assertEquals(20_000, config.randomPortRange().orElseThrow().minPort());
        assertEquals(21_000, config.randomPortRange().orElseThrow().maxPort());
        assertTrue(config.spaFallback().isEmpty());
    }

    @Test
    void fixedPortDisablesRandomRange() {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(20_000, 20_010)
                .port(20_100)
                .build();

        assertEquals(20_100, config.fixedPort().orElseThrow());
        assertFalse(config.randomPortRange().isPresent());
    }

    @Test
    void spaFallbackIsNormalizedWithLeadingSlash() {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .spaFallback("assets/my-mod-id/web/index.html")
                .build();

        assertEquals("/assets/my-mod-id/web/index.html", config.spaFallback().orElseThrow());
    }

    @Test
    void baseUrlSchemeCanBeConfigured() {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .baseUrlScheme("https")
                .build();

        assertEquals("https", config.baseUrlScheme());
    }

    @Test
    void equivalentConfigsAreEqual() {
        GrapheneHttpConfig left = GrapheneHttpConfig.builder()
                .bindHost("127.0.0.1")
                .randomPortInRange(20_000, 20_010)
                .spaFallback("/assets/test/index.html")
                .build();
        GrapheneHttpConfig right = GrapheneHttpConfig.builder()
                .bindHost("127.0.0.1")
                .randomPortInRange(20_000, 20_010)
                .spaFallback("/assets/test/index.html")
                .build();
        GrapheneHttpConfig different = GrapheneHttpConfig.builder()
                .bindHost("localhost")
                .randomPortInRange(20_000, 20_010)
                .spaFallback("/assets/test/index.html")
                .build();

        assertEquals(left, right);
        assertNotEquals(left, different);
    }
}
