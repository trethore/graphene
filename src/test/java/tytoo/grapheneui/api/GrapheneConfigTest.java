package tytoo.grapheneui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneConfigTest {
    @Test
    void defaultsDoNotEnableHttpServer() {
        GrapheneConfig config = GrapheneConfig.defaults();

        assertTrue(config.http().isEmpty());
    }

    @Test
    void builderCanEnableHttpServer() {
        GrapheneHttpConfig httpConfig = GrapheneHttpConfig.builder()
                .randomPortInRange(20_000, 20_010)
                .build();
        GrapheneConfig config = GrapheneConfig.builder()
                .http(httpConfig)
                .build();

        assertTrue(config.http().isPresent());
    }
}
