package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;

import java.util.Objects;

abstract class AbstractGrapheneAssetUrls implements GrapheneAssetUrls {
    protected static final String ASSET_HOST = "assets";
    protected static final String PATH_DELIMITER = "/";
    private final String defaultNamespace;

    protected AbstractGrapheneAssetUrls(String defaultNamespace) {
        this.defaultNamespace = normalizeNamespace(defaultNamespace);
    }

    protected abstract String rootPrefix();

    @Override
    public final String asset(String path) {
        return asset(defaultNamespace, path);
    }

    @Override
    public final String asset(String namespace, String path) {
        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedPath = normalizePath(path);
        String resolvedRootPrefix = Objects.requireNonNull(rootPrefix(), "rootPrefix");
        return resolvedRootPrefix + normalizedNamespace + PATH_DELIMITER + normalizedPath;
    }

    @Override
    public final String asset(Identifier assetId) {
        Identifier identifier = Objects.requireNonNull(assetId, "assetId");
        return asset(identifier.getNamespace(), identifier.getPath());
    }

    protected final String normalizePath(String path) {
        String normalizedPath = Objects.requireNonNull(path, "path").trim();
        while (normalizedPath.startsWith(PATH_DELIMITER)) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }

    protected final String normalizeNamespace(String namespace) {
        String normalizedNamespace = Objects.requireNonNull(namespace, "namespace").trim();
        while (normalizedNamespace.startsWith(PATH_DELIMITER)) {
            normalizedNamespace = normalizedNamespace.substring(1);
        }

        while (normalizedNamespace.endsWith(PATH_DELIMITER)) {
            normalizedNamespace = normalizedNamespace.substring(0, normalizedNamespace.length() - 1);
        }

        return normalizedNamespace;
    }
}
