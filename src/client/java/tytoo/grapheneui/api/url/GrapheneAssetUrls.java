package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;

public interface GrapheneAssetUrls {
    String asset(String path);

    String asset(String namespace, String path);

    String asset(Identifier assetId);
}
