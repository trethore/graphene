package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;

/**
 * Utility class for constructing runtime HTTP URLs for classpath assets.
 */
public final class GrapheneHttpUrls {
    private static final GrapheneHttpUrlsSupport SUPPORT = new GrapheneHttpUrlsSupport(GrapheneCore.ID);

    private GrapheneHttpUrls() {
    }

    public static String asset(String path) {
        return SUPPORT.asset(path);
    }

    public static String asset(String namespace, String path) {
        return SUPPORT.asset(namespace, path);
    }

    public static String asset(Identifier assetId) {
        return SUPPORT.asset(assetId);
    }

    public static GrapheneAssetUrls assets() {
        return SUPPORT;
    }

    public static GrapheneAssetUrls assets(String namespace) {
        return new GrapheneHttpUrlsSupport(namespace);
    }

    private static final class GrapheneHttpUrlsSupport extends AbstractGrapheneAssetUrls {
        private GrapheneHttpUrlsSupport(String defaultNamespace) {
            super(defaultNamespace);
        }

        private static String requireHttpBaseUrl() {
            GrapheneHttpServer server = GrapheneCore.runtime().httpServer();
            if (!server.isRunning()) {
                throw new IllegalStateException("Graphene HTTP server is not running. Configure GrapheneHttpConfig and register Graphene with GrapheneCore.register(modId, config).");
            }

            return server.baseUrl();
        }

        @Override
        protected String rootPrefix() {
            return requireHttpBaseUrl() + PATH_DELIMITER + ASSET_HOST + PATH_DELIMITER;
        }
    }
}
