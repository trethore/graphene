package tytoo.grapheneui.api;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class GrapheneConfig {
    private static final Path DEFAULT_JCEF_DOWNLOAD_PATH = Path.of("./jcef");
    private static final GrapheneConfig DEFAULT = builder().build();
    private static final String JCEF_DOWNLOAD_PATH_NAME = "jcefDownloadPath";
    private static final String EXTENSION_FOLDER_NAME = "extensionFolder";
    private static final String HTTP_CONFIG_NAME = "httpConfig";

    private final Path jcefDownloadPath;
    private final List<Path> extensionFolders;
    private final GrapheneHttpConfig httpConfig;

    private GrapheneConfig(Builder builder) {
        this.jcefDownloadPath = builder.jcefDownloadPath == null
                ? null
                : normalizePath(builder.jcefDownloadPath, JCEF_DOWNLOAD_PATH_NAME);
        this.extensionFolders = List.copyOf(builder.extensionFolders.stream().sorted().toList());
        this.httpConfig = builder.httpConfig;
    }

    public static GrapheneConfig defaults() {
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

    public Optional<Path> jcefDownloadPath() {
        return Optional.ofNullable(jcefDownloadPath);
    }

    public Path resolvedJcefDownloadPath() {
        return jcefDownloadPath == null ? DEFAULT_JCEF_DOWNLOAD_PATH : jcefDownloadPath;
    }

    public List<Path> extensionFolders() {
        return extensionFolders;
    }

    public Optional<GrapheneHttpConfig> http() {
        return Optional.ofNullable(httpConfig);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof GrapheneConfig other)) {
            return false;
        }

        return Objects.equals(jcefDownloadPath, other.jcefDownloadPath)
                && Objects.equals(extensionFolders, other.extensionFolders)
                && Objects.equals(httpConfig, other.httpConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jcefDownloadPath, extensionFolders, httpConfig);
    }

    public static final class Builder {
        private final LinkedHashSet<Path> extensionFolders = new LinkedHashSet<>();
        private Path jcefDownloadPath;
        private GrapheneHttpConfig httpConfig;

        private Builder() {
        }

        public Builder jcefDownloadPath(Path jcefDownloadPath) {
            this.jcefDownloadPath = normalizePath(jcefDownloadPath, JCEF_DOWNLOAD_PATH_NAME);
            return this;
        }

        public Builder jcefDownloadPath(String jcefDownloadPath) {
            return jcefDownloadPath(Path.of(Objects.requireNonNull(jcefDownloadPath, JCEF_DOWNLOAD_PATH_NAME)));
        }

        public Builder extensionFolder(Path extensionFolder) {
            this.extensionFolders.add(normalizePath(extensionFolder, EXTENSION_FOLDER_NAME));
            return this;
        }

        public Builder extensionFolder(String extensionFolder) {
            return extensionFolder(Path.of(Objects.requireNonNull(extensionFolder, EXTENSION_FOLDER_NAME)));
        }

        public Builder disableExtensions() {
            this.extensionFolders.clear();
            return this;
        }

        public Builder http(GrapheneHttpConfig httpConfig) {
            this.httpConfig = Objects.requireNonNull(httpConfig, HTTP_CONFIG_NAME);
            return this;
        }

        public Builder disableHttp() {
            this.httpConfig = null;
            return this;
        }

        public GrapheneConfig build() {
            return new GrapheneConfig(this);
        }
    }
}
