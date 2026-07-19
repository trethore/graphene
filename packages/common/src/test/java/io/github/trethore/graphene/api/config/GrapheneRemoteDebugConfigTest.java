package io.github.trethore.graphene.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GrapheneRemoteDebugConfigTest {
  @Test
  void defaultsToEnabledWithRandomPort() {
    GrapheneRemoteDebugConfig config = GrapheneRemoteDebugConfig.builder().build();

    assertTrue(config.enabled());
    assertTrue(config.fixedPort().isEmpty());
    assertTrue(config.allowedOrigins().isEmpty());
  }

  @Test
  void fixedPortAndOriginsCanBeConfigured() {
    GrapheneRemoteDebugConfig config =
        GrapheneRemoteDebugConfig.builder()
            .port(20_000)
            .allowedOrigins(" https://example.test ")
            .build();

    assertEquals(20_000, config.fixedPort().orElseThrow());
    assertEquals("https://example.test", config.allowedOrigins().orElseThrow());
  }

  @Test
  void disabledConfigClearsOtherSettings() {
    GrapheneRemoteDebugConfig config =
        GrapheneRemoteDebugConfig.builder()
            .port(20_000)
            .allowedOrigins("https://example.test")
            .disable()
            .build();

    assertFalse(config.enabled());
    assertTrue(config.fixedPort().isEmpty());
    assertTrue(config.allowedOrigins().isEmpty());
    assertEquals(GrapheneRemoteDebugConfig.disabled(), config);
  }

  @Test
  void invalidSettingsAreRejected() {
    GrapheneRemoteDebugConfig.Builder builder = GrapheneRemoteDebugConfig.builder();

    assertThrows(IllegalArgumentException.class, () -> builder.port(1023));
    assertThrows(IllegalArgumentException.class, () -> builder.allowedOrigins(" "));
  }
}
