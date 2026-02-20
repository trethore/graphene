package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;

/**
 * Utility class for constructing and normalizing "app:" URLs for loading assets.
 * The URLs have the format "app://assets/{namespace}/{path}".
 */
public final class GrapheneAppUrls {
    public static final String SCHEME = "app";
    public static final String ASSET_HOST = AbstractGrapheneAssetUrls.ASSET_HOST;

    private static final GrapheneAppUrlsSupport SUPPORT = new GrapheneAppUrlsSupport();

    private GrapheneAppUrls() {
    }

    public static String asset(String path) {
        return SUPPORT.buildAssetUrl(path);
    }

    public static String asset(String namespace, String path) {
        return SUPPORT.buildAssetUrl(namespace, path);
    }

    public static String asset(Identifier assetId) {
        return SUPPORT.buildAssetUrl(assetId);
    }

    public static String normalizeResourcePath(String url) {
        return SUPPORT.normalizeAssetResourcePath(url);
    }

    private static final class GrapheneAppUrlsSupport extends AbstractGrapheneAssetUrls {
        private static final String ROOT_PREFIX = SCHEME + "://" + ASSET_HOST + "/";

        private GrapheneAppUrlsSupport() {
            super(SCHEME, ROOT_PREFIX);
        }
    }
}
