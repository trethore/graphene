package tytoo.grapheneui.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Locale;

@SuppressWarnings("unused")
public final class GrapheneHttpConfig {
    private static final String DEFAULT_BIND_HOST = "127.0.0.1";
    private static final String DEFAULT_BASE_URL_SCHEME = "http";
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

    private final String baseUrlScheme;
    private final String bindHost;
    private final Integer fixedPort;
    private final PortRange randomPortRange;
    private final String spaFallback;

    private GrapheneHttpConfig(Builder builder) {
        this.baseUrlScheme = normalizeBaseUrlScheme(builder.baseUrlScheme);
        this.bindHost = normalizeBindHost(builder.bindHost);
        this.fixedPort = builder.fixedPort;
        this.randomPortRange = builder.randomPortRange;
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

    private static String normalizeBaseUrlScheme(String baseUrlScheme) {
        String normalizedBaseUrlScheme = Objects.requireNonNull(baseUrlScheme, "baseUrlScheme").trim();
        if (normalizedBaseUrlScheme.isBlank()) {
            throw new IllegalArgumentException("baseUrlScheme must not be blank");
        }

        if (!normalizedBaseUrlScheme.equalsIgnoreCase("http") && !normalizedBaseUrlScheme.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("baseUrlScheme must be either http or https");
        }

        return normalizedBaseUrlScheme.toLowerCase(Locale.ROOT);
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

    private static int requireValidPort(int port, String name) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException(name + " must be between " + MIN_PORT + " and " + MAX_PORT);
        }

        return port;
    }

    public String bindHost() {
        return bindHost;
    }

    public String baseUrlScheme() {
        return baseUrlScheme;
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

    public static final class Builder {
        private String baseUrlScheme = DEFAULT_BASE_URL_SCHEME;
        private String bindHost = DEFAULT_BIND_HOST;
        private Integer fixedPort;
        private PortRange randomPortRange = new PortRange(20_000, 21_000);
        private String spaFallback;

        private Builder() {
        }

        public Builder bindHost(String bindHost) {
            this.bindHost = normalizeBindHost(bindHost);
            return this;
        }

        public Builder baseUrlScheme(String baseUrlScheme) {
            this.baseUrlScheme = normalizeBaseUrlScheme(baseUrlScheme);
            return this;
        }

        public Builder port(int port) {
            this.fixedPort = requireValidPort(port, "port");
            this.randomPortRange = null;
            return this;
        }

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

        public Builder spaFallback(String spaFallback) {
            this.spaFallback = normalizeSpaFallback(spaFallback);
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
