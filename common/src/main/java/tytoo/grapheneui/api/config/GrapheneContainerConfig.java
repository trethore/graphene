package tytoo.grapheneui.api.config;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class GrapheneContainerConfig {
    private static final GrapheneContainerConfig DEFAULT = builder().build();
    private static final String HTTP_CONFIG_NAME = "httpConfig";

    private final GrapheneHttpConfig httpConfig;

    private GrapheneContainerConfig(Builder builder) {
        this.httpConfig = builder.httpConfig;
    }

    public static GrapheneContainerConfig defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof GrapheneContainerConfig other)) {
            return false;
        }

        return Objects.equals(httpConfig, other.httpConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpConfig);
    }

    public Optional<GrapheneHttpConfig> http() {
        return Optional.ofNullable(httpConfig);
    }

    public static final class Builder {
        private GrapheneHttpConfig httpConfig;

        private Builder() {
        }

        public Builder http(GrapheneHttpConfig httpConfig) {
            this.httpConfig = Objects.requireNonNull(httpConfig, HTTP_CONFIG_NAME);
            return this;
        }

        public Builder disableHttp() {
            this.httpConfig = null;
            return this;
        }

        public GrapheneContainerConfig build() {
            return new GrapheneContainerConfig(this);
        }
    }
}
