package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.config.GrapheneRemoteDebugConfig;
import io.github.trethore.jcefgithub.CefAppBuilder;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrapheneCefInstallerTest {
  @TempDir Path temporaryDirectory;

  @Test
  void configuresPortableSettingsAndFixedDebugPort() {
    GrapheneGlobalConfig config =
        GrapheneGlobalConfig.builder()
            .browserRuntimePath(temporaryDirectory)
            .remoteDebugging(GrapheneRemoteDebugConfig.builder().port(9333).build())
            .build();

    CefAppBuilder builder = GrapheneCefInstaller.createBuilder(config);

    assertEquals(9333, builder.getCefSettings().remote_debugging_port);
    assertTrue(builder.getCefSettings().windowless_rendering_enabled);
    assertTrue(builder.getJcefArgs().contains("--disable-extensions"));
    assertTrue(Path.of(builder.getCefSettings().cache_path).startsWith(temporaryDirectory));
  }

  @Test
  void disablesRemoteDebuggingByDefault() {
    GrapheneGlobalConfig config =
        GrapheneGlobalConfig.builder().browserRuntimePath(temporaryDirectory).build();

    CefAppBuilder builder = GrapheneCefInstaller.createBuilder(config);

    assertEquals(0, builder.getCefSettings().remote_debugging_port);
  }
}
