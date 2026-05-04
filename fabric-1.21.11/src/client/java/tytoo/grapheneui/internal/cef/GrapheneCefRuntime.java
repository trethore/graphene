package tytoo.grapheneui.internal.cef;

import io.github.trethore.jcefgithub.CefAppBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.util.Util;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.api.surface.BrowserSurface;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeBrowser;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeOptions;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.browser.surface.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.internal.cef.startup.GrapheneNativeDownloadOverlay;
import tytoo.grapheneui.internal.cef.startup.GrapheneNativeDownloadState;
import tytoo.grapheneui.internal.devtools.GrapheneDevToolsResolver;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.event.GrapheneTitleEventBus;
import tytoo.grapheneui.internal.mc.McClient;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class GrapheneCefRuntime implements GrapheneRuntime {
    private final GrapheneDevToolsResolver devToolsResolver = new GrapheneDevToolsResolver();
    private final GrapheneBrowserSurfaceManager surfaceManager;
    private final GrapheneCefRuntimeCore core;
    private boolean shutdownLifecycleRegistered;

    public GrapheneCefRuntime(GrapheneBrowserSurfaceManager surfaceManager) {
        this(surfaceManager, GrapheneBridgeOptions.defaults());
    }

    public GrapheneCefRuntime(GrapheneBrowserSurfaceManager surfaceManager, GrapheneBridgeOptions bridgeOptions) {
        this.surfaceManager = Objects.requireNonNull(surfaceManager, "surfaceManager");
        this.core = new GrapheneCefRuntimeCore(new FabricRuntimeHooks(), Objects.requireNonNull(bridgeOptions, "bridgeOptions"));
    }

    public void initialize(GrapheneGlobalConfig globalConfig, Map<String, GrapheneContainerConfig> containerConfigs) {
        registerShutdownLifecycle();
        core.initialize(globalConfig, containerConfigs);
    }

    public CompletableFuture<Void> initializeAsync(
            GrapheneGlobalConfig globalConfig,
            Map<String, GrapheneContainerConfig> containerConfigs
    ) {
        registerShutdownLifecycle();
        return core.initializeAsync(globalConfig, containerConfigs);
    }

    public CefClient requireClient() {
        return core.requireClient();
    }

    public GrapheneLoadEventBus getLoadEventBus() {
        return core.getLoadEventBus();
    }

    public GrapheneTitleEventBus getTitleEventBus() {
        return core.getTitleEventBus();
    }

    public GrapheneBridge attachBridge(GrapheneBridgeBrowser browser) {
        return core.attachBridge(browser);
    }

    public void detachBridge(CefBrowser browser) {
        core.detachBridge(browser);
    }

    public void onNavigationRequested(CefBrowser browser) {
        core.onNavigationRequested(browser);
    }

    public void ensureBootstrap(CefBrowser browser) {
        core.ensureBootstrap(browser);
    }

    @Override
    public int getRemoteDebuggingPort() {
        return core.getRemoteDebuggingPort();
    }

    @Override
    public CompletableFuture<URI> resolveDevToolsUri(BrowserSurface surface) {
        BrowserSurface validatedSurface = Objects.requireNonNull(surface, "surface");
        return devToolsResolver.resolveUri(getRemoteDebuggingPort(), validatedSurface.currentUrl());
    }

    @Override
    public CompletableFuture<URI> openDevTools(BrowserSurface surface) {
        return resolveDevToolsUri(surface).thenApply(devToolsUri -> {
            Util.getPlatform().openUri(devToolsUri);
            return devToolsUri;
        });
    }

    @Override
    public GrapheneHttpServer httpServer() {
        return core.httpServer();
    }

    @Override
    public boolean isInitialized() {
        return core.isInitialized();
    }

    public void shutdown() {
        core.shutdown();
    }

    private void registerShutdownLifecycle() {
        if (shutdownLifecycleRegistered) {
            return;
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(ignoredClient -> shutdown());
        shutdownLifecycleRegistered = true;
    }

    private final class FabricRuntimeHooks implements GrapheneCefRuntimeCore.RuntimeHooks {
        @Override
        public void configureCefApp(CefAppBuilder cefAppBuilder, GrapheneGlobalConfig globalConfig) {
            GrapheneCefAppHandler appHandler = new GrapheneCefAppHandler(globalConfig.fileSystemAccessMode());
            cefAppBuilder.setAppHandler(appHandler);
        }

        @Override
        public void configureCefClient(
                CefClient cefClient,
                GrapheneLoadEventBus loadEventBus,
                GrapheneTitleEventBus titleEventBus,
                GrapheneBridgeRuntime bridgeRuntime,
                GrapheneCefBrowserShutdownTracker browserShutdownTracker
        ) {
            GrapheneCefClientConfig.configure(
                    cefClient,
                    loadEventBus,
                    titleEventBus,
                    bridgeRuntime,
                    browserShutdownTracker,
                    McClient::runOnMainThread
            );
        }

        @Override
        public void showStartupProgress(GrapheneNativeDownloadState downloadState) {
            GrapheneNativeDownloadOverlay downloadOverlay = new GrapheneNativeDownloadOverlay(downloadState);
            McClient.runOnMainThread(() -> {
                if (McClient.currentOverlay() != null) {
                    return;
                }

                McClient.setOverlay(downloadOverlay);
            });
        }

        @Override
        public void dismissStartupProgress(GrapheneNativeDownloadState downloadState) {
            downloadState.reset();
            McClient.runOnMainThread(() -> {
                if (McClient.currentOverlay() instanceof GrapheneNativeDownloadOverlay) {
                    McClient.setOverlay(null);
                }
            });
        }

        @Override
        public void closeSurfaces() {
            surfaceManager.closeAll();
        }

        @Override
        public boolean isMainThread() {
            return McClient.mc().isSameThread();
        }

        @Override
        public tytoo.grapheneui.internal.core.GrapheneMainThreadExecutor mainThreadExecutor() {
            return McClient::runOnMainThread;
        }
    }
}
