package io.github.trethore.graphene.api.config;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for a consumer-scoped HTTP asset mount. By default the shared server binds to
 * {@code 127.0.0.1} using an available port from {@code 20000} through {@code 21000}.
 */
@SuppressWarnings("unused")
public final class GrapheneHttpConfig {
  private static final String DEFAULT_BIND_HOST = "127.0.0.1";
  private static final int MIN_PORT = 1024;
  private static final int MAX_PORT = 65535;
  private static final String FILE_ROOT_NAME = "fileRoot";

  private final String bindHost;
  private final Integer fixedPort;
  private final PortRange randomPortRange;
  private final Path fileRoot;
  private final String spaFallback;

  private GrapheneHttpConfig(Builder builder) {
    this.bindHost = normalizeBindHost(builder.bindHost);
    this.fixedPort = builder.fixedPort;
    this.randomPortRange = builder.randomPortRange;
    this.fileRoot = normalizeFileRoot(builder.fileRoot);
    this.spaFallback = normalizeSpaFallback(builder.spaFallback);
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String normalizeBindHost(String bindHost) {
    String normalizedBindHost = Objects.requireNonNull(bindHost, "bindHost").trim();
    if (normalizedBindHost.isBlank()) {
      throw new IllegalArgumentException("bindHost must not be blank");
    }

    return normalizedBindHost;
  }

  private static String normalizeSpaFallback(String spaFallback) {
    if (spaFallback == null) {
      return null;
    }

    String normalizedSpaFallback = spaFallback.trim();
    if (normalizedSpaFallback.isBlank()) {
      throw new IllegalArgumentException("spaFallback must not be blank");
    }

    if (!normalizedSpaFallback.startsWith("/")) {
      normalizedSpaFallback = "/" + normalizedSpaFallback;
    }

    return normalizedSpaFallback;
  }

  private static Path normalizeFileRoot(Path fileRoot) {
    if (fileRoot == null) {
      return null;
    }

    Path normalizedFileRoot = fileRoot.normalize();
    if (normalizedFileRoot.toString().isBlank()) {
      throw new IllegalArgumentException("fileRoot must not be blank");
    }

    return normalizedFileRoot.toAbsolutePath().normalize();
  }

  private static int requireValidPort(int port, String name) {
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException(
          name + " must be between " + MIN_PORT + " and " + MAX_PORT);
    }

    return port;
  }

  public String bindHost() {
    return bindHost;
  }

  public Optional<Integer> fixedPort() {
    return Optional.ofNullable(fixedPort);
  }

  public Optional<PortRange> randomPortRange() {
    return Optional.ofNullable(randomPortRange);
  }

  public Optional<String> spaFallback() {
    return Optional.ofNullable(spaFallback);
  }

  public Optional<Path> fileRoot() {
    return Optional.ofNullable(fileRoot);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (!(object instanceof GrapheneHttpConfig other)) {
      return false;
    }

    return Objects.equals(bindHost, other.bindHost)
        && Objects.equals(fixedPort, other.fixedPort)
        && Objects.equals(randomPortRange, other.randomPortRange)
        && Objects.equals(fileRoot, other.fileRoot)
        && Objects.equals(spaFallback, other.spaFallback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bindHost, fixedPort, randomPortRange, fileRoot, spaFallback);
  }

  /** Builds consumer-scoped HTTP server configuration. */
  public static final class Builder {
    private String bindHost = DEFAULT_BIND_HOST;
    private Integer fixedPort;
    private PortRange randomPortRange = new PortRange(20_000, 21_000);
    private Path fileRoot;
    private String spaFallback;

    private Builder() {}

    public Builder bindHost(String bindHost) {
      this.bindHost = normalizeBindHost(bindHost);
      return this;
    }

    /** Selects one fixed port from {@code 1024} through {@code 65535}. */
    public Builder port(int port) {
      this.fixedPort = requireValidPort(port, "port");
      this.randomPortRange = null;
      return this;
    }

    /** Selects an available port from the inclusive range. */
    public Builder randomPortInRange(int minPort, int maxPort) {
      int validatedMinPort = requireValidPort(minPort, "minPort");
      int validatedMaxPort = requireValidPort(maxPort, "maxPort");
      if (validatedMinPort > validatedMaxPort) {
        throw new IllegalArgumentException("minPort must be <= maxPort");
      }

      this.randomPortRange = new PortRange(validatedMinPort, validatedMaxPort);
      this.fixedPort = null;
      return this;
    }

    /** Sets the asset path served when no requested HTTP resource exists. */
    public Builder spaFallback(String spaFallback) {
      this.spaFallback = normalizeSpaFallback(spaFallback);
      return this;
    }

    /** Sets an optional filesystem root whose files take precedence over packaged assets. */
    public Builder fileRoot(Path fileRoot) {
      this.fileRoot = normalizeFileRoot(Objects.requireNonNull(fileRoot, FILE_ROOT_NAME));
      return this;
    }

    public Builder fileRoot(String fileRoot) {
      return fileRoot(Path.of(Objects.requireNonNull(fileRoot, FILE_ROOT_NAME)));
    }

    public Builder clearFileRoot() {
      this.fileRoot = null;
      return this;
    }

    public Builder clearSpaFallback() {
      this.spaFallback = null;
      return this;
    }

    public GrapheneHttpConfig build() {
      return new GrapheneHttpConfig(this);
    }
  }

  /** Inclusive port range used for random HTTP server port selection. */
  public record PortRange(int minPort, int maxPort) {
    public PortRange {
      requireValidPort(minPort, "minPort");
      requireValidPort(maxPort, "maxPort");
      if (minPort > maxPort) {
        throw new IllegalArgumentException("minPort must be <= maxPort");
      }
    }
  }
}
