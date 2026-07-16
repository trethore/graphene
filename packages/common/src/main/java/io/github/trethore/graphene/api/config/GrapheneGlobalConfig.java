package io.github.trethore.graphene.api.config;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class GrapheneGlobalConfig {
  private static final Path DEFAULT_BROWSER_RUNTIME_PATH =
      Path.of("./graphene/browser-runtime").normalize();
  private static final GrapheneGlobalConfig DEFAULT = builder().build();
  private static final String BROWSER_RUNTIME_PATH_NAME = "browserRuntimePath";
  private static final String EXTENSION_FOLDER_NAME = "extensionFolder";
  private static final String REMOTE_DEBUG_CONFIG_NAME = "remoteDebugConfig";
  private static final String BROWSER_FILE_ACCESS_POLICY_NAME = "browserFileAccessPolicy";

  private final Path browserRuntimePath;
  private final List<Path> extensionFolders;
  private final GrapheneRemoteDebugConfig remoteDebugConfig;
  private final BrowserFileAccessPolicy browserFileAccessPolicy;

  private GrapheneGlobalConfig(Builder builder) {
    this.browserRuntimePath =
        builder.browserRuntimePath == null
            ? null
            : normalizePath(builder.browserRuntimePath, BROWSER_RUNTIME_PATH_NAME);
    this.extensionFolders = List.copyOf(builder.extensionFolders.stream().sorted().toList());
    this.remoteDebugConfig = builder.remoteDebugConfig;
    this.browserFileAccessPolicy =
        Objects.requireNonNull(builder.browserFileAccessPolicy, BROWSER_FILE_ACCESS_POLICY_NAME);
  }

  public static GrapheneGlobalConfig defaults() {
    return DEFAULT;
  }

  public static Builder builder() {
    return new Builder();
  }

  private static Path normalizePath(Path path, String argumentName) {
    Path validatedPath = Objects.requireNonNull(path, argumentName).normalize();
    if (validatedPath.toString().isBlank()) {
      throw new IllegalArgumentException(argumentName + " must not be blank");
    }

    return validatedPath;
  }

  public Optional<Path> browserRuntimePath() {
    return Optional.ofNullable(browserRuntimePath);
  }

  public Path resolvedBrowserRuntimePath() {
    return browserRuntimePath == null ? DEFAULT_BROWSER_RUNTIME_PATH : browserRuntimePath;
  }

  public List<Path> extensionFolders() {
    return extensionFolders;
  }

  public Optional<GrapheneRemoteDebugConfig> remoteDebugging() {
    return Optional.ofNullable(remoteDebugConfig);
  }

  public BrowserFileAccessPolicy browserFileAccessPolicy() {
    return browserFileAccessPolicy;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (!(object instanceof GrapheneGlobalConfig other)) {
      return false;
    }

    return Objects.equals(browserRuntimePath, other.browserRuntimePath)
        && Objects.equals(extensionFolders, other.extensionFolders)
        && Objects.equals(remoteDebugConfig, other.remoteDebugConfig)
        && browserFileAccessPolicy == other.browserFileAccessPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        browserRuntimePath, extensionFolders, remoteDebugConfig, browserFileAccessPolicy);
  }

  public static final class Builder {
    private final LinkedHashSet<Path> extensionFolders = new LinkedHashSet<>();
    private Path browserRuntimePath;
    private GrapheneRemoteDebugConfig remoteDebugConfig;
    private BrowserFileAccessPolicy browserFileAccessPolicy = BrowserFileAccessPolicy.DENY;

    private Builder() {}

    public Builder browserRuntimePath(Path browserRuntimePath) {
      this.browserRuntimePath = normalizePath(browserRuntimePath, BROWSER_RUNTIME_PATH_NAME);
      return this;
    }

    public Builder browserRuntimePath(String browserRuntimePath) {
      return browserRuntimePath(
          Path.of(Objects.requireNonNull(browserRuntimePath, BROWSER_RUNTIME_PATH_NAME)));
    }

    public Builder extensionFolder(Path extensionFolder) {
      this.extensionFolders.add(normalizePath(extensionFolder, EXTENSION_FOLDER_NAME));
      return this;
    }

    public Builder extensionFolder(String extensionFolder) {
      return extensionFolder(
          Path.of(Objects.requireNonNull(extensionFolder, EXTENSION_FOLDER_NAME)));
    }

    public Builder clearExtensionFolders() {
      this.extensionFolders.clear();
      return this;
    }

    public Builder remoteDebugging(GrapheneRemoteDebugConfig remoteDebugConfig) {
      this.remoteDebugConfig = Objects.requireNonNull(remoteDebugConfig, REMOTE_DEBUG_CONFIG_NAME);
      return this;
    }

    public Builder disableRemoteDebugging() {
      this.remoteDebugConfig = GrapheneRemoteDebugConfig.disabled();
      return this;
    }

    public Builder browserFileAccessPolicy(BrowserFileAccessPolicy browserFileAccessPolicy) {
      this.browserFileAccessPolicy =
          Objects.requireNonNull(browserFileAccessPolicy, BROWSER_FILE_ACCESS_POLICY_NAME);
      return this;
    }

    public Builder allowBrowserFileAccess() {
      this.browserFileAccessPolicy = BrowserFileAccessPolicy.ALLOW;
      return this;
    }

    public Builder denyBrowserFileAccess() {
      this.browserFileAccessPolicy = BrowserFileAccessPolicy.DENY;
      return this;
    }

    public GrapheneGlobalConfig build() {
      return new GrapheneGlobalConfig(this);
    }
  }
}
