package tytoo.grapheneui.api;

import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.api.url.GrapheneAppUrls;
import tytoo.grapheneui.api.url.GrapheneAssetUrls;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;
import tytoo.grapheneui.api.url.GrapheneHttpUrls;

public final class GrapheneMod {
    private final String id;

    GrapheneMod(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public GrapheneRuntime runtime() {
        return GrapheneCore.runtime();
    }

    public GrapheneAssetUrls appAssets() {
        return GrapheneAppUrls.assets(id);
    }

    public GrapheneAssetUrls classpathAssets() {
        return GrapheneClasspathUrls.assets(id);
    }

    public GrapheneAssetUrls httpAssets() {
        return GrapheneHttpUrls.assets(id);
    }
}
