package tytoo.grapheneui.api.runtime;

import tytoo.grapheneui.api.surface.BrowserSurface;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public interface GrapheneRuntime {
    boolean isInitialized();

    int getRemoteDebuggingPort();

    CompletableFuture<URI> resolveDevToolsUri(BrowserSurface surface);

    CompletableFuture<URI> openDevTools(BrowserSurface surface);

    GrapheneHttpServer httpServer();
}
