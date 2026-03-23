package tytoo.grapheneui.api.config;

import java.util.Objects;

@SuppressWarnings("unused")
public final class GrapheneConfig {
    private static final GrapheneConfig DEFAULT = builder().build();
    private static final String CONTAINER_CONFIG_NAME = "containerConfig";
    private static final String GLOBAL_CONFIG_NAME = "globalConfig";

    private final GrapheneContainerConfig containerConfig;
    private final GrapheneGlobalConfig globalConfig;

    private GrapheneConfig(Builder builder) {
        this.containerConfig = Objects.requireNonNull(builder.containerConfig, CONTAINER_CONFIG_NAME);
        this.globalConfig = Objects.requireNonNull(builder.globalConfig, GLOBAL_CONFIG_NAME);
    }

    public static GrapheneConfig defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public GrapheneContainerConfig container() {
        return containerConfig;
    }

    public GrapheneGlobalConfig global() {
        return globalConfig;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof GrapheneConfig other)) {
            return false;
        }

        return Objects.equals(containerConfig, other.containerConfig)
                && Objects.equals(globalConfig, other.globalConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerConfig, globalConfig);
    }

    public static final class Builder {
        private GrapheneContainerConfig containerConfig = GrapheneContainerConfig.defaults();
        private GrapheneGlobalConfig globalConfig = GrapheneGlobalConfig.defaults();

        private Builder() {
        }

        public Builder container(GrapheneContainerConfig containerConfig) {
            this.containerConfig = Objects.requireNonNull(containerConfig, CONTAINER_CONFIG_NAME);
            return this;
        }

        public Builder global(GrapheneGlobalConfig globalConfig) {
            this.globalConfig = Objects.requireNonNull(globalConfig, GLOBAL_CONFIG_NAME);
            return this;
        }

        public GrapheneConfig build() {
            return new GrapheneConfig(this);
        }
    }
}
