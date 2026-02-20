package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;
import tytoo.grapheneui.api.GrapheneCore;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

abstract class AbstractGrapheneAssetUrls {
    protected static final String ASSET_HOST = "assets";
    private static final String PATH_DELIMITER = "/";
    private final String rootPrefix;
    private final String schemePrefix;
    private final String assetsPrefix;

    protected AbstractGrapheneAssetUrls(String scheme, String rootPrefix) {
        String normalizedScheme = Objects.requireNonNull(scheme, "scheme");
        this.rootPrefix = Objects.requireNonNull(rootPrefix, "rootPrefix");
        this.schemePrefix = normalizedScheme + ":";
        this.assetsPrefix = ASSET_HOST + PATH_DELIMITER;
    }

    protected final String buildAssetUrl(String path) {
        return buildAssetUrl(GrapheneCore.ID, path);
    }

    protected final String buildAssetUrl(String namespace, String path) {
        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedPath = normalizePath(path);
        return rootPrefix + normalizedNamespace + PATH_DELIMITER + normalizedPath;
    }

    protected final String buildAssetUrl(Identifier assetId) {
        Identifier identifier = Objects.requireNonNull(assetId, "assetId");
        return buildAssetUrl(identifier.getNamespace(), identifier.getPath());
    }

    protected final String normalizeAssetResourcePath(String url) {
        String value = Objects.requireNonNull(url, "url");
        if (!value.regionMatches(true, 0, schemePrefix, 0, schemePrefix.length())) {
            return "";
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ignored) {
            // Invalid URL format cannot map to a classpath resource path.
            return "";
        }

        String host = uri.getHost();
        String rawPath = uri.getRawPath();
        String normalizedPath = normalizePath(rawPath == null ? "" : rawPath);

        if (host == null || host.isBlank()) {
            if (!normalizedPath.startsWith(assetsPrefix)) {
                return "";
            }

            return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
        }

        String normalizedHost = normalizeNamespace(host);
        String resourcePath;
        if (ASSET_HOST.equalsIgnoreCase(normalizedHost)) {
            resourcePath = assetsPrefix + normalizedPath;
        } else {
            resourcePath = assetsPrefix + normalizedHost + PATH_DELIMITER + normalizedPath;
        }

        return URLDecoder.decode(resourcePath, StandardCharsets.UTF_8);
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
