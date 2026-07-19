package io.github.trethore.graphene.internal.runtime;

import io.github.trethore.graphene.api.config.BrowserFileAccessPolicy;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfigConflictException;
import io.github.trethore.graphene.api.config.GrapheneRemoteDebugConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

final class GrapheneGlobalConfigResolver {
  private GrapheneGlobalConfigResolver() {}

  static GrapheneGlobalConfig resolve(Map<String, GrapheneConfig> consumerConfigs) {
    List<Map.Entry<String, GrapheneConfig>> consumers =
        Objects.requireNonNull(consumerConfigs, "consumerConfigs").entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();
    List<OwnedValue<Path>> runtimePaths = new ArrayList<>();
    List<OwnedValue<GrapheneRemoteDebugConfig>> remoteDebugConfigs = new ArrayList<>();
    List<OwnedValue<BrowserFileAccessPolicy>> fileAccessPolicies = new ArrayList<>();
    GrapheneGlobalConfig.Builder builder = GrapheneGlobalConfig.builder();

    for (Map.Entry<String, GrapheneConfig> consumer : consumers) {
      String owner = consumer.getKey();
      GrapheneGlobalConfig config = Objects.requireNonNull(consumer.getValue(), owner).global();
      config
          .browserRuntimePath()
          .map(GrapheneGlobalConfigResolver::absolutePath)
          .ifPresent(path -> runtimePaths.add(new OwnedValue<>(owner, path)));
      config
          .remoteDebugging()
          .ifPresent(remoteDebug -> remoteDebugConfigs.add(new OwnedValue<>(owner, remoteDebug)));
      fileAccessPolicies.add(new OwnedValue<>(owner, config.browserFileAccessPolicy()));
      config.extensionFolders().stream()
          .map(GrapheneGlobalConfigResolver::absolutePath)
          .forEach(builder::extensionFolder);
    }

    Path runtimePath =
        compatibleValue(
            runtimePaths,
            GrapheneGlobalConfigConflictException.Setting.BROWSER_RUNTIME_PATH,
            Path::toString);
    GrapheneRemoteDebugConfig remoteDebug =
        compatibleValue(
            remoteDebugConfigs,
            GrapheneGlobalConfigConflictException.Setting.REMOTE_DEBUGGING,
            GrapheneGlobalConfigResolver::describeRemoteDebugging);
    BrowserFileAccessPolicy fileAccessPolicy =
        compatibleValue(
            fileAccessPolicies,
            GrapheneGlobalConfigConflictException.Setting.BROWSER_FILE_ACCESS_POLICY,
            BrowserFileAccessPolicy::name);

    if (runtimePath != null) {
      builder.browserRuntimePath(runtimePath);
    }
    if (remoteDebug != null) {
      builder.remoteDebugging(remoteDebug);
    }
    if (fileAccessPolicy != null) {
      builder.browserFileAccessPolicy(fileAccessPolicy);
    }
    return builder.build();
  }

  private static Path absolutePath(Path path) {
    return path.toAbsolutePath().normalize();
  }

  private static String describeRemoteDebugging(GrapheneRemoteDebugConfig config) {
    if (!config.enabled()) {
      return "DISABLED";
    }
    String port = config.fixedPort().map(String::valueOf).orElse("random port");
    String origins = config.allowedOrigins().orElse("default origins");
    return "ENABLED (" + port + ", " + origins + ")";
  }

  private static <T> T compatibleValue(
      List<OwnedValue<T>> contributions,
      GrapheneGlobalConfigConflictException.Setting setting,
      Function<T, String> valueFormatter) {
    if (contributions.isEmpty()) {
      return null;
    }

    T selectedValue = contributions.getFirst().value();
    boolean conflict =
        contributions.stream()
            .skip(1)
            .anyMatch(contribution -> !Objects.equals(selectedValue, contribution.value()));
    if (conflict) {
      List<GrapheneGlobalConfigConflictException.Contribution> reportedContributions =
          contributions.stream()
              .map(
                  contribution ->
                      new GrapheneGlobalConfigConflictException.Contribution(
                          contribution.owner(), valueFormatter.apply(contribution.value())))
              .toList();
      throw new GrapheneGlobalConfigConflictException(setting, reportedContributions);
    }
    return selectedValue;
  }

  private record OwnedValue<T>(String owner, T value) {}
}
