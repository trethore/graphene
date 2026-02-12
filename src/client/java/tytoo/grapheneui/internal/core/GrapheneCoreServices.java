package tytoo.grapheneui.internal.core;

import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.browser.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.internal.cef.GrapheneCefRuntime;

public final class GrapheneCoreServices {
    private static final GrapheneCoreServices INSTANCE = new GrapheneCoreServices();

    private final GrapheneBrowserSurfaceManager surfaceManager;
    private final GrapheneCefRuntime cefRuntime;

    private GrapheneCoreServices() {
        this.surfaceManager = new GrapheneBrowserSurfaceManager();
        this.cefRuntime = new GrapheneCefRuntime(surfaceManager);
    }

    public static GrapheneCoreServices get() {
        return INSTANCE;
    }

    public GrapheneRuntime runtime() {
        return cefRuntime;
    }

    public GrapheneCefRuntime runtimeInternal() {
        return cefRuntime;
    }

    public GrapheneBrowserSurfaceManager surfaceManager() {
        return surfaceManager;
    }
}
