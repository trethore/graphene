package io.github.trethore.graphene.api.url;

public interface GrapheneAssetUrls {
  String url(String path);

  String url(String namespace, String path);

  String url(AssetId assetId);
}
