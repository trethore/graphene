package io.github.trethore.graphene.internal.url;

import io.github.trethore.graphene.api.url.AssetId;
import io.github.trethore.graphene.api.url.GrapheneAssetUrls;

public final class GrapheneAppUrls {
  public static final String SCHEME = "app";

  private static final GrapheneAppUrlsSupport SUPPORT = new GrapheneAppUrlsSupport("grapheneui");

  private GrapheneAppUrls() {}

  public static String url(String namespace, String path) {
    return SUPPORT.url(namespace, path);
  }

  public static String url(AssetId assetId) {
    return SUPPORT.url(assetId);
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
