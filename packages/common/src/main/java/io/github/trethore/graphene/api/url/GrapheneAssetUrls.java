package io.github.trethore.graphene.api.url;

/** Creates URLs for assets in a Graphene-managed source. */
public interface GrapheneAssetUrls {
  String url(String path);

  String url(String namespace, String path);

  String url(AssetId assetId);
}
