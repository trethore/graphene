package io.github.trethore.graphene.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class GrapheneConfigTest {
  @Test
  void defaultsUseEmptyContainerAndDenyBrowserFileAccess() {
    GrapheneConfig config = GrapheneConfig.defaults();

    assertTrue(config.container().http().isEmpty());
    assertTrue(config.global().browserRuntimePath().isEmpty());
    assertEquals(
        Path.of("./graphene/browser-runtime").normalize(),
        config.global().resolvedBrowserRuntimePath());
    assertTrue(config.global().extensionFolders().isEmpty());
    assertTrue(config.global().remoteDebugging().isEmpty());
    assertEquals(BrowserFileAccessPolicy.DENY, config.global().browserFileAccessPolicy());
  }

  @Test
  void builderConfiguresContainerAndGlobalSettings() {
    GrapheneHttpConfig httpConfig =
        GrapheneHttpConfig.builder().randomPortInRange(20_000, 20_010).build();
    GrapheneRemoteDebugConfig remoteDebugConfig =
        GrapheneRemoteDebugConfig.builder()
            .randomPort()
            .allowedOrigins("https://example.test")
            .build();

    GrapheneConfig config =
        GrapheneConfig.builder()
            .container(GrapheneContainerConfig.builder().http(httpConfig).build())
            .global(
                GrapheneGlobalConfig.builder()
                    .browserRuntimePath("./custom-runtime")
                    .extensionFolder("./extensions/mod-a")
                    .extensionFolder(Path.of("./extensions/mod-b"))
                    .remoteDebugging(remoteDebugConfig)
                    .allowBrowserFileAccess()
                    .build())
            .build();

    assertEquals(httpConfig, config.container().http().orElseThrow());
    assertEquals(Path.of("custom-runtime"), config.global().browserRuntimePath().orElseThrow());
    assertEquals(2, config.global().extensionFolders().size());
    assertEquals(remoteDebugConfig, config.global().remoteDebugging().orElseThrow());
    assertEquals(BrowserFileAccessPolicy.ALLOW, config.global().browserFileAccessPolicy());
  }

  @Test
  void extensionFoldersAreNormalizedDeduplicatedAndSorted() {
    GrapheneGlobalConfig config =
        GrapheneGlobalConfig.builder()
            .extensionFolder("extensions/z")
            .extensionFolder("extensions/a/../a")
            .extensionFolder("extensions/a")
            .build();

    assertEquals(
        java.util.List.of(Path.of("extensions/a"), Path.of("extensions/z")),
        config.extensionFolders());
  }

  @Test
  void disabledSettingsAreExplicit() {
    GrapheneGlobalConfig config =
        GrapheneGlobalConfig.builder()
            .extensionFolder("extensions/a")
            .clearExtensionFolders()
            .remoteDebugging(GrapheneRemoteDebugConfig.builder().port(20_000).build())
            .disableRemoteDebugging()
            .allowBrowserFileAccess()
            .denyBrowserFileAccess()
            .build();

    assertTrue(config.extensionFolders().isEmpty());
    assertFalse(config.remoteDebugging().orElseThrow().enabled());
    assertEquals(BrowserFileAccessPolicy.DENY, config.browserFileAccessPolicy());
  }

  @Test
  void equivalentConfigsAreEqual() {
    GrapheneConfig left =
        GrapheneConfig.builder()
            .global(GrapheneGlobalConfig.builder().browserRuntimePath("runtime/./").build())
            .build();
    GrapheneConfig right =
        GrapheneConfig.builder()
            .global(GrapheneGlobalConfig.builder().browserRuntimePath("runtime").build())
            .build();

    assertEquals(left, right);
    assertEquals(left.hashCode(), right.hashCode());
    assertNotEquals(left, GrapheneConfig.defaults());
  }

  @Test
  void blankRuntimeAndExtensionPathsAreRejected() {
    GrapheneGlobalConfig.Builder builder = GrapheneGlobalConfig.builder();

    assertThrows(IllegalArgumentException.class, () -> builder.browserRuntimePath(""));
    assertThrows(IllegalArgumentException.class, () -> builder.extensionFolder(""));
  }
}
