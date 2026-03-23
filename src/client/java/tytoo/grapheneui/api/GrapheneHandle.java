package tytoo.grapheneui.api;

import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.api.url.GrapheneAssetUrls;

@SuppressWarnings("unused")
public interface GrapheneHandle {
    String id();

    GrapheneConfig config();

    GrapheneContainerConfig containerConfig();

    GrapheneGlobalConfig globalConfig();

    GrapheneGlobalConfig effectiveGlobalConfig();

    GrapheneRuntime runtime();

    GrapheneAssetUrls appAssets();

    GrapheneAssetUrls classpathAssets();

    GrapheneAssetUrls httpAssets();

    String httpUrl(String path);
}
