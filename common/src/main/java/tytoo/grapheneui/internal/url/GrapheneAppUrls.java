package tytoo.grapheneui.internal.url;

import tytoo.grapheneui.api.GrapheneConstants;
import tytoo.grapheneui.api.url.GrapheneAssetUrls;

public final class GrapheneAppUrls {
    public static final String SCHEME = "app";

    private static final GrapheneAppUrlsSupport SUPPORT = new GrapheneAppUrlsSupport(GrapheneConstants.ID);

    private GrapheneAppUrls() {
    }

    public static String asset(String namespace, String path) {
        return SUPPORT.asset(namespace, path);
    }

    public static String asset(Object assetId) {
        return SUPPORT.asset(assetId);
    }

    public static GrapheneAssetUrls assets() {
        return SUPPORT;
    }

    public static GrapheneAssetUrls assets(String namespace) {
        return new GrapheneAppUrlsSupport(namespace);
    }

    public static String normalizeResourcePath(String url) {
        return SUPPORT.normalizeAssetResourcePath(url);
    }

    private static final class GrapheneAppUrlsSupport extends AbstractGrapheneSchemedAssetUrls {
        private static final String ROOT_PREFIX = SCHEME + "://" + ASSET_HOST + "/";

        private GrapheneAppUrlsSupport(String defaultNamespace) {
            super(SCHEME, defaultNamespace);
        }

        @Override
        protected String rootPrefix() {
            return ROOT_PREFIX;
        }
    }
}
