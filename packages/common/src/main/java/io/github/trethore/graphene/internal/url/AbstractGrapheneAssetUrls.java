package io.github.trethore.graphene.internal.url;

import io.github.trethore.graphene.api.url.AssetId;
import io.github.trethore.graphene.api.url.GrapheneAssetUrls;
import java.util.Objects;

abstract class AbstractGrapheneAssetUrls implements GrapheneAssetUrls {
  protected static final String ASSET_HOST = "assets";
  protected static final String PATH_DELIMITER = "/";

  private final String defaultNamespace;

  protected AbstractGrapheneAssetUrls(String defaultNamespace) {
    this.defaultNamespace = AssetId.normalizeNamespace(defaultNamespace);
  }

  protected abstract String rootPrefix();

  @Override
  public final String url(String path) {
    return url(defaultNamespace, path);
  }

  @Override
  public final String url(String namespace, String path) {
    return url(AssetId.of(namespace, path));
  }

  @Override
  public final String url(AssetId assetId) {
    AssetId validatedAssetId = Objects.requireNonNull(assetId, "assetId");
    String resolvedRootPrefix = Objects.requireNonNull(rootPrefix(), "rootPrefix");
    return resolvedRootPrefix
        + validatedAssetId.namespace()
        + PATH_DELIMITER
        + validatedAssetId.path();
  }
}
