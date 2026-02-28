package tytoo.grapheneui.api.config;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class GrapheneRemoteDebugConfig {
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;
    private static final GrapheneRemoteDebugConfig DISABLED = GrapheneRemoteDebugConfig.builder().disable().build();

    private final boolean enabled;
    private final Integer fixedPort;
    private final String allowedOrigins;

    private GrapheneRemoteDebugConfig(Builder builder) {
        this.enabled = builder.enabled;
        if (!builder.enabled) {
            this.fixedPort = null;
            this.allowedOrigins = null;
            return;
        }

        this.fixedPort = builder.fixedPort == null ? null : requireValidPort(builder.fixedPort);
        this.allowedOrigins = builder.allowedOrigins == null ? null : normalizeAllowedOrigins(builder.allowedOrigins);
    }

    public static GrapheneRemoteDebugConfig disabled() {
        return DISABLED;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String normalizeAllowedOrigins(String allowedOrigins) {
        String normalizedAllowedOrigins = Objects.requireNonNull(allowedOrigins, "allowedOrigins").trim();
        if (normalizedAllowedOrigins.isBlank()) {
            throw new IllegalArgumentException("allowedOrigins must not be blank");
        }

        return normalizedAllowedOrigins;
    }

    private static int requireValidPort(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("port must be between " + MIN_PORT + " and " + MAX_PORT);
        }

        return port;
    }

    public boolean enabled() {
        return enabled;
    }

    public Optional<Integer> fixedPort() {
        return Optional.ofNullable(fixedPort);
    }

    public Optional<String> allowedOrigins() {
        return Optional.ofNullable(allowedOrigins);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof GrapheneRemoteDebugConfig other)) {
            return false;
        }

        return enabled == other.enabled
                && Objects.equals(fixedPort, other.fixedPort)
                && Objects.equals(allowedOrigins, other.allowedOrigins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, fixedPort, allowedOrigins);
    }

    public static final class Builder {
        private boolean enabled = true;
        private Integer fixedPort;
        private String allowedOrigins;

        private Builder() {
        }

        public Builder enable() {
            this.enabled = true;
            return this;
        }

        public Builder disable() {
            this.enabled = false;
            this.fixedPort = null;
            this.allowedOrigins = null;
            return this;
        }

        public Builder port(int port) {
            this.enabled = true;
            this.fixedPort = requireValidPort(port);
            return this;
        }

        public Builder randomPort() {
            this.enabled = true;
            this.fixedPort = null;
            return this;
        }

        public Builder allowedOrigins(String allowedOrigins) {
            this.enabled = true;
            this.allowedOrigins = normalizeAllowedOrigins(allowedOrigins);
            return this;
        }

        public GrapheneRemoteDebugConfig build() {
            return new GrapheneRemoteDebugConfig(this);
        }
    }
}
