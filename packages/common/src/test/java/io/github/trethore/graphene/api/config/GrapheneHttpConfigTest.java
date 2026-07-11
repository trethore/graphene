package io.github.trethore.graphene.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class GrapheneHttpConfigTest {
  @Test
  void defaultsUseLoopbackAndDefaultRange() {
    GrapheneHttpConfig config = GrapheneHttpConfig.builder().build();

    assertEquals("http", config.baseUrlScheme());
    assertEquals("127.0.0.1", config.bindHost());
    assertTrue(config.fixedPort().isEmpty());
    assertEquals(20_000, config.randomPortRange().orElseThrow().minPort());
    assertEquals(21_000, config.randomPortRange().orElseThrow().maxPort());
    assertTrue(config.fileRoot().isEmpty());
    assertTrue(config.spaFallback().isEmpty());
  }

  @Test
  void fixedPortDisablesRandomRange() {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(20_000, 20_010).port(20_100).build();

    assertEquals(20_100, config.fixedPort().orElseThrow());
    assertFalse(config.randomPortRange().isPresent());
  }

  @Test
  void fileRootAndSpaFallbackAreNormalized() {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder()
            .fileRoot(Path.of("ui/../ui"))
            .spaFallback("assets/example/web/index.html")
            .build();

    assertEquals(Path.of("ui").toAbsolutePath().normalize(), config.fileRoot().orElseThrow());
    assertEquals("/assets/example/web/index.html", config.spaFallback().orElseThrow());
  }

  @Test
  void invalidSettingsAreRejected() {
    GrapheneHttpConfig.Builder builder = GrapheneHttpConfig.builder();

    assertThrows(IllegalArgumentException.class, () -> builder.bindHost(" "));
    assertThrows(IllegalArgumentException.class, () -> builder.baseUrlScheme("ftp"));
    assertThrows(IllegalArgumentException.class, () -> builder.port(1023));
    assertThrows(IllegalArgumentException.class, () -> builder.randomPortInRange(21_000, 20_000));
    assertThrows(IllegalArgumentException.class, () -> builder.fileRoot(""));
    assertThrows(IllegalArgumentException.class, () -> builder.spaFallback(" "));
  }

  @Test
  void equivalentConfigsAreEqual() {
    GrapheneHttpConfig left =
        GrapheneHttpConfig.builder()
            .bindHost("127.0.0.1")
            .baseUrlScheme("HTTP")
            .randomPortInRange(20_000, 20_010)
            .fileRoot("ui")
            .spaFallback("/assets/test/index.html")
            .build();
    GrapheneHttpConfig right =
        GrapheneHttpConfig.builder()
            .randomPortInRange(20_000, 20_010)
            .fileRoot("ui/./")
            .spaFallback("assets/test/index.html")
            .build();
    GrapheneHttpConfig different =
        GrapheneHttpConfig.builder()
            .bindHost("localhost")
            .randomPortInRange(20_000, 20_010)
            .fileRoot("ui")
            .spaFallback("/assets/test/index.html")
            .build();

    assertEquals(left, right);
    assertEquals(left.hashCode(), right.hashCode());
    assertNotEquals(left, different);
  }
}
