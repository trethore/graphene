package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;

/**
 * Utility class for constructing and normalizing "classpath:" URLs for loading assets from the classpath.
 * The URLs have the format "classpath:///assets/{namespace}/{path}".
 */
public final class GrapheneClasspathUrls {
    public static final String SCHEME = "classpath";

    private static final GrapheneClasspathUrlsSupport SUPPORT = new GrapheneClasspathUrlsSupport();

    private GrapheneClasspathUrls() {
    }

    /**
     * Constructs a classpath URL for an asset with the specified path in the default namespace.
     */
    public static String asset(String path) {
        return SUPPORT.asset(path);
    }

    /**
     * Constructs a classpath URL for an asset with the specified namespace and path.
     */
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

    private static final class GrapheneClasspathUrlsSupport extends AbstractGrapheneSchemedAssetUrls {
        private static final String ROOT_PREFIX = SCHEME + ":///" + ASSET_HOST + "/";

        private GrapheneClasspathUrlsSupport() {
            super(SCHEME);
        }

        @Override
        protected String rootPrefix() {
            return ROOT_PREFIX;
        }
    }
}
