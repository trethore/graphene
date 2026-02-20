package tytoo.grapheneui.api.url;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

abstract class AbstractGrapheneSchemedAssetUrls extends AbstractGrapheneAssetUrls {
    private final String assetsPrefix;
    private final String schemePrefix;

    protected AbstractGrapheneSchemedAssetUrls(String scheme, String defaultNamespace) {
        super(defaultNamespace);
        this.schemePrefix = Objects.requireNonNull(scheme, "scheme") + ":";
        this.assetsPrefix = ASSET_HOST + PATH_DELIMITER;
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
            // Invalid URL format cannot map to an asset resource path.
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
}
