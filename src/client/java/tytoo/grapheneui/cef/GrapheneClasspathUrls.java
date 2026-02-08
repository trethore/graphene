package tytoo.grapheneui.cef;

import tytoo.grapheneui.GrapheneCore;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class GrapheneClasspathUrls {
    public static final String SCHEME = "classpath";

    private static final String SCHEME_PREFIX = SCHEME + ":";
    private static final String ROOT_PREFIX = SCHEME_PREFIX + "///";

    private GrapheneClasspathUrls() {
    }

    public static String asset(String path) {
        String normalizedPath = normalizePath(path);
        return ROOT_PREFIX + "assets/" + GrapheneCore.ID + "/" + normalizedPath;
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
}
