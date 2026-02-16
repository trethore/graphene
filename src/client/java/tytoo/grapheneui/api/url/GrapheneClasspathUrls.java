package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;
import tytoo.grapheneui.api.GrapheneCore;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Utility class for constructing and normalizing "classpath:" URLs for loading assets from the classpath.
 * The URLs have the format "classpath:///assets/{namespace}/{path}".
 */
public final class GrapheneClasspathUrls {
    public static final String SCHEME = "classpath";

    private static final String SCHEME_PREFIX = SCHEME + ":";
    private static final String ROOT_PREFIX = SCHEME_PREFIX + "///";

    private GrapheneClasspathUrls() {
    }

    /**
     * Constructs a classpath URL for an asset with the specified path in the default namespace.
     */
    public static String asset(String path) {
        return asset(GrapheneCore.ID, path);
    }

    /**
     * Constructs a classpath URL for an asset with the specified namespace and path.
     */
    public static String asset(String namespace, String path) {
        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedPath = normalizePath(path);
        return ROOT_PREFIX + "assets/" + normalizedNamespace + "/" + normalizedPath;
    }

    public static String asset(Identifier assetId) {
        Identifier identifier = Objects.requireNonNull(assetId, "assetId");
        return asset(identifier.getNamespace(), identifier.getPath());
    }

    public static String normalizeResourcePath(String url) {
        String value = Objects.requireNonNull(url, "url");
        if (!value.regionMatches(true, 0, SCHEME_PREFIX, 0, SCHEME_PREFIX.length())) {
            return "";
        }

        String path = value.substring(SCHEME_PREFIX.length());
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex >= 0) {
            path = path.substring(0, fragmentIndex);
        }

        if (path.startsWith("//")) {
            path = path.substring(2);
        }

        String normalizedPath = normalizePath(path);
        return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
    }

    private static String normalizePath(String path) {
        String normalizedPath = Objects.requireNonNull(path, "path").trim();
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }

    private static String normalizeNamespace(String namespace) {
        String normalizedNamespace = Objects.requireNonNull(namespace, "namespace").trim();
        while (normalizedNamespace.startsWith("/")) {
            normalizedNamespace = normalizedNamespace.substring(1);
        }

        while (normalizedNamespace.endsWith("/")) {
            normalizedNamespace = normalizedNamespace.substring(0, normalizedNamespace.length() - 1);
        }

        return normalizedNamespace;
    }
}
