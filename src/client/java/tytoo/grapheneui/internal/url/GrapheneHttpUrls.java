package tytoo.grapheneui.internal.url;

import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.api.url.GrapheneAssetUrls;

public final class GrapheneHttpUrls {
    private static final String PATH_DELIMITER = "/";
    private static final String MODS_ROOT = "mods";
    private static final GrapheneHttpUrlsSupport SUPPORT = new GrapheneHttpUrlsSupport(GrapheneCore.ID);

    private GrapheneHttpUrls() {
    }

    public static String asset(String namespace, String path) {
        return SUPPORT.asset(namespace, path);
    }

    public static GrapheneAssetUrls assets() {
        return SUPPORT;
    }

    public static GrapheneAssetUrls assets(String namespace) {
        return new GrapheneHttpUrlsSupport(namespace);
    }

    public static String modUrl(String modId, String path) {
        String normalizedModId = SUPPORT.normalizeNamespace(modId);
        String normalizedPath = SUPPORT.normalizePath(path);
        return requireHttpBaseUrl() + PATH_DELIMITER + MODS_ROOT + PATH_DELIMITER + normalizedModId + PATH_DELIMITER + normalizedPath;
    }

    private static String requireHttpBaseUrl() {
        GrapheneHttpServer server = GrapheneCore.runtime().httpServer();
        if (!server.isRunning()) {
            throw new IllegalStateException(
                    "Graphene HTTP server is not running. Configure GrapheneContainerConfig.http(...) and register Graphene with GrapheneCore.register(...)."
            );
        }

        return server.baseUrl();
    }

    private static final class GrapheneHttpUrlsSupport extends AbstractGrapheneAssetUrls {
        private GrapheneHttpUrlsSupport(String defaultNamespace) {
            super(defaultNamespace);
        }

        @Override
        protected String rootPrefix() {
            return GrapheneHttpUrls.requireHttpBaseUrl() + PATH_DELIMITER + ASSET_HOST + PATH_DELIMITER;
        }
    }
}
