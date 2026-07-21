package io.github.trethore.graphene.internal.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.config.BrowserFileAccessPolicy;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfigConflictException;
import io.github.trethore.graphene.api.config.GrapheneRemoteDebugConfig;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class GrapheneGlobalConfigResolverTest {
  @Test
  void mergesCompatibleContributions() {
    Path runtimePath = Path.of("runtime").toAbsolutePath().normalize();
    Path sharedExtensions = Path.of("extensions/shared").toAbsolutePath().normalize();
    Path alphaExtensions = Path.of("extensions/alpha").toAbsolutePath().normalize();
    GrapheneRemoteDebugConfig remoteDebug =
        GrapheneRemoteDebugConfig.builder().port(20_000).allowedOrigins("http://localhost").build();

    GrapheneGlobalConfig resolved =
        GrapheneGlobalConfigResolver.resolve(
            Map.of(
                "beta",
                config(
                    GrapheneGlobalConfig.builder()
                        .browserRuntimePath(runtimePath)
                        .extensionFolder(sharedExtensions)
                        .remoteDebugging(remoteDebug)
                        .allowBrowserFileAccess()
                        .build()),
                "alpha",
                config(
                    GrapheneGlobalConfig.builder()
                        .browserRuntimePath("runtime")
                        .extensionFolder("extensions/shared")
                        .extensionFolder(alphaExtensions)
                        .remoteDebugging(remoteDebug)
                        .allowBrowserFileAccess()
                        .build())));

    assertEquals(runtimePath, resolved.browserRuntimePath().orElseThrow());
    assertEquals(List.of(alphaExtensions, sharedExtensions), resolved.extensionFolders());
    assertEquals(remoteDebug, resolved.remoteDebugging().orElseThrow());
    assertEquals(BrowserFileAccessPolicy.ALLOW, resolved.browserFileAccessPolicy());
  }

  @Test
  void acceptsAnEnabledRemoteDebugContributionWhenOtherConsumersDoNotContribute() {
    GrapheneRemoteDebugConfig remoteDebug =
        GrapheneRemoteDebugConfig.builder().randomPort().build();

    GrapheneGlobalConfig resolved =
        GrapheneGlobalConfigResolver.resolve(
            Map.of(
                "alpha", GrapheneConfig.defaults(),
                "beta",
                    config(GrapheneGlobalConfig.builder().remoteDebugging(remoteDebug).build())));

    assertEquals(remoteDebug, resolved.remoteDebugging().orElseThrow());
  }

  @Test
  void reportsEveryRuntimePathOwnerInDeterministicOrder() {
    LinkedHashMap<String, GrapheneConfig> configs = new LinkedHashMap<>();
    configs.put("gamma", runtimePathConfig("runtime-a"));
    configs.put("beta", runtimePathConfig("runtime-b"));
    configs.put("alpha", runtimePathConfig("runtime-a"));

    GrapheneGlobalConfigConflictException exception =
        assertThrows(
            GrapheneGlobalConfigConflictException.class,
            () -> GrapheneGlobalConfigResolver.resolve(configs));

    assertEquals(
        GrapheneGlobalConfigConflictException.Setting.BROWSER_RUNTIME_PATH, exception.setting());
    assertEquals(
        List.of("alpha", "beta", "gamma"),
        exception.contributions().stream()
            .map(GrapheneGlobalConfigConflictException.Contribution::consumerId)
            .toList());
    assertTrue(exception.getMessage().contains("alpha ->"));
    assertTrue(exception.getMessage().contains("beta ->"));
    assertTrue(exception.getMessage().contains("gamma ->"));
  }

  @Test
  void conflictsOnDifferentExplicitRemoteDebuggingConfigurations() {
    GrapheneConfig enabled =
        config(
            GrapheneGlobalConfig.builder()
                .remoteDebugging(GrapheneRemoteDebugConfig.builder().port(20_000).build())
                .build());
    GrapheneConfig disabled =
        config(GrapheneGlobalConfig.builder().disableRemoteDebugging().build());
    Map<String, GrapheneConfig> configs = Map.of("enabled", enabled, "disabled", disabled);

    GrapheneGlobalConfigConflictException exception =
        assertThrows(
            GrapheneGlobalConfigConflictException.class,
            () -> GrapheneGlobalConfigResolver.resolve(configs));

    assertEquals(
        GrapheneGlobalConfigConflictException.Setting.REMOTE_DEBUGGING, exception.setting());
    assertEquals(
        List.of("disabled", "enabled"),
        exception.contributions().stream()
            .map(GrapheneGlobalConfigConflictException.Contribution::consumerId)
            .toList());
    assertEquals("DISABLED", exception.contributions().getFirst().value());
  }

  @Test
  void browserFileAccessRequiresUnanimousAllow() {
    GrapheneConfig allowed =
        config(GrapheneGlobalConfig.builder().allowBrowserFileAccess().build());
    Map<String, GrapheneConfig> configs =
        Map.of("beta", GrapheneConfig.defaults(), "alpha", allowed);

    GrapheneGlobalConfigConflictException exception =
        assertThrows(
            GrapheneGlobalConfigConflictException.class,
            () -> GrapheneGlobalConfigResolver.resolve(configs));

    assertEquals(
        GrapheneGlobalConfigConflictException.Setting.BROWSER_FILE_ACCESS_POLICY,
        exception.setting());
    assertEquals(
        List.of(
            new GrapheneGlobalConfigConflictException.Contribution("alpha", "ALLOW"),
            new GrapheneGlobalConfigConflictException.Contribution("beta", "DENY")),
        exception.contributions());
  }

  private static GrapheneConfig runtimePathConfig(String path) {
    return config(GrapheneGlobalConfig.builder().browserRuntimePath(path).build());
  }

  private static GrapheneConfig config(GrapheneGlobalConfig globalConfig) {
    return GrapheneConfig.builder().global(globalConfig).build();
  }
}
