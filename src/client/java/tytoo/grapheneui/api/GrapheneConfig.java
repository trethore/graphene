package tytoo.grapheneui.api;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class GrapheneConfig {
    private static final Path DEFAULT_JCEF_DOWNLOAD_PATH = Path.of("./jcef");
    private static final GrapheneConfig DEFAULT = builder().build();
    private static final String JCEF_DOWNLOAD_PATH_NAME = "jcefDownloadPath";
    private static final String EXTENSION_FOLDER_NAME = "extensionFolder";

    private final Path jcefDownloadPath;
    private final Path extensionFolder;

    private GrapheneConfig(Builder builder) {
        this.jcefDownloadPath = normalizePath(builder.jcefDownloadPath, JCEF_DOWNLOAD_PATH_NAME);
        this.extensionFolder = builder.extensionFolder == null
                ? null
                : normalizePath(builder.extensionFolder, EXTENSION_FOLDER_NAME);
    }

    public static GrapheneConfig defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Path jcefDownloadPath() {
        return jcefDownloadPath;
    }

    public Optional<Path> extensionFolder() {
        return Optional.ofNullable(extensionFolder);
    }

    private static Path normalizePath(Path path, String argumentName) {
        Path validatedPath = Objects.requireNonNull(path, argumentName).normalize();
        if (validatedPath.toString().isBlank()) {
            throw new IllegalArgumentException(argumentName + " must not be blank");
        }

        return validatedPath;
    }

    public static final class Builder {
        private Path jcefDownloadPath = DEFAULT_JCEF_DOWNLOAD_PATH;
        private Path extensionFolder;

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
            this.extensionFolder = normalizePath(extensionFolder, EXTENSION_FOLDER_NAME);
            return this;
        }

        public Builder extensionFolder(String extensionFolder) {
            return extensionFolder(Path.of(Objects.requireNonNull(extensionFolder, EXTENSION_FOLDER_NAME)));
        }

        public Builder disableExtensions() {
            this.extensionFolder = null;
            return this;
        }

        public GrapheneConfig build() {
            return new GrapheneConfig(this);
        }
    }
}
