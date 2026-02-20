package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;

/**
 * Utility class for constructing and normalizing "app:" URLs for loading assets.
 * The URLs have the format "app://assets/{namespace}/{path}".
 */
public final class GrapheneAppUrls {
    public static final String SCHEME = "app";

    private static final GrapheneAppUrlsSupport SUPPORT = new GrapheneAppUrlsSupport();

    private GrapheneAppUrls() {
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

    public static String normalizeResourcePath(String url) {
        return SUPPORT.normalizeAssetResourcePath(url);
    }

    private static final class GrapheneAppUrlsSupport extends AbstractGrapheneSchemedAssetUrls {
        private static final String ROOT_PREFIX = SCHEME + "://" + ASSET_HOST + "/";

        private GrapheneAppUrlsSupport() {
            super(SCHEME);
        }

        @Override
        protected String rootPrefix() {
            return ROOT_PREFIX;
        }
    }
}
