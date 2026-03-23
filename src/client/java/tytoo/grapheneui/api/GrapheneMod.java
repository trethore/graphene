package tytoo.grapheneui.api;

import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.api.url.GrapheneAssetUrls;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;
import tytoo.grapheneui.internal.url.GrapheneAppUrls;
import tytoo.grapheneui.internal.url.GrapheneHttpUrls;

import java.util.Objects;

public final class GrapheneMod implements GrapheneHandle {
    private final String id;
    private final GrapheneConfig config;

    GrapheneMod(String id, GrapheneConfig config) {
        this.id = id;
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public GrapheneConfig config() {
        return config;
    }

    @Override
    public GrapheneContainerConfig containerConfig() {
        return config.container();
    }

    @Override
    public GrapheneGlobalConfig globalConfig() {
        return config.global();
    }

    @Override
    public GrapheneGlobalConfig effectiveGlobalConfig() {
        return GrapheneCore.globalConfig();
    }

    @Override
    public GrapheneRuntime runtime() {
        return GrapheneCore.runtime();
    }

    @Override
    public GrapheneAssetUrls appAssets() {
        return GrapheneAppUrls.assets(id);
    }

    @Override
    public GrapheneAssetUrls classpathAssets() {
        return GrapheneClasspathUrls.assets(id);
    }

    @Override
    public GrapheneAssetUrls httpAssets() {
        return GrapheneHttpUrls.assets(id);
    }

    @Override
    public String httpUrl(String path) {
        return GrapheneHttpUrls.modUrl(id, path);
    }
}
