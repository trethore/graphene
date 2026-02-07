package tytoo.grapheneui;

import tytoo.grapheneui.browser.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.cef.GrapheneCefRuntime;

final class GrapheneCoreServices {
    private final GrapheneBrowserSurfaceManager surfaceManager;
    private final GrapheneCefRuntime cefRuntime;

    GrapheneCoreServices() {
        this.surfaceManager = new GrapheneBrowserSurfaceManager();
        this.cefRuntime = new GrapheneCefRuntime(surfaceManager);
    }

    GrapheneBrowserSurfaceManager surfaceManager() {
        return surfaceManager;
    }

    GrapheneCefRuntime cefRuntime() {
        return cefRuntime;
    }
}
