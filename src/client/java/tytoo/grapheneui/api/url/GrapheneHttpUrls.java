package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;

import java.util.Objects;

/**
 * Utility class for constructing runtime HTTP URLs for classpath assets.
 */
public final class GrapheneHttpUrls {
    private static final String PATH_DELIMITER = "/";

    private GrapheneHttpUrls() {
    }

    public static String asset(String path) {
        return asset(GrapheneCore.ID, path);
    }

    public static String asset(String namespace, String path) {
        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedPath = normalizePath(path);
        return requireHttpBaseUrl()
                + PATH_DELIMITER
                + GrapheneAppUrls.ASSET_HOST
                + PATH_DELIMITER
                + normalizedNamespace
                + PATH_DELIMITER
                + normalizedPath;
    }

    public static String asset(Identifier assetId) {
        Identifier identifier = Objects.requireNonNull(assetId, "assetId");
        return asset(identifier.getNamespace(), identifier.getPath());
    }

    private static String requireHttpBaseUrl() {
        GrapheneHttpServer server = GrapheneCore.runtime().httpServer();
        if (!server.isRunning()) {
            throw new IllegalStateException("Graphene HTTP server is not running. Configure GrapheneHttpConfig and call GrapheneCore.init().");
        }

        return server.baseUrl();
    }

    private static String normalizePath(String path) {
        String normalizedPath = Objects.requireNonNull(path, "path").trim();
        while (normalizedPath.startsWith(PATH_DELIMITER)) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }

    private static String normalizeNamespace(String namespace) {
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
