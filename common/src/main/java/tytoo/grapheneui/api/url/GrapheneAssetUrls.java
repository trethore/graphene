package tytoo.grapheneui.api.url;

public interface GrapheneAssetUrls {
    String asset(String path);

    String asset(String namespace, String path);

    String asset(Object assetId);
}
