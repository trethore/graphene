package tytoo.grapheneui.cef;

import me.tytoo.jcefgithub.CefAppBuilder;
import me.tytoo.jcefgithub.CefInitializationException;
import me.tytoo.jcefgithub.UnsupportedPlatformException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.cef.CefApp;
import org.cef.CefClient;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.bridge.GrapheneBridge;
import tytoo.grapheneui.bridge.internal.GrapheneBridgeRuntime;
import tytoo.grapheneui.browser.GrapheneBrowser;
import tytoo.grapheneui.browser.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.event.GrapheneLoadEventBus;
import tytoo.grapheneui.platform.GraphenePlatform;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class GrapheneCefRuntime {
    private static final long CEF_SHUTDOWN_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long CEF_SHUTDOWN_POLL_INTERVAL_MILLIS = 25;

    private final Object lock = new Object();
    private final GrapheneBrowserSurfaceManager surfaceManager;
    private final GrapheneLoadEventBus loadEventBus = new GrapheneLoadEventBus();
    private final GrapheneBridgeRuntime bridgeRuntime = new GrapheneBridgeRuntime();
    private boolean initialized;
    private boolean shutdownHookRegistered;
    private CefApp cefApp;
    private CefClient cefClient;
    private int remoteDebuggingPort = -1;

    public GrapheneCefRuntime(GrapheneBrowserSurfaceManager surfaceManager) {
        this.surfaceManager = Objects.requireNonNull(surfaceManager, "surfaceManager");
    }

    private static void logStartupConfiguration(CefAppBuilder cefAppBuilder) {
        GrapheneCore.LOGGER.info(
                "Initializing CEF on platform linux={}, wayland={}, subprocess={}, args={}",
                GraphenePlatform.isLinux(),
                GraphenePlatform.isWaylandSession(),
                cefAppBuilder.getCefSettings().browser_subprocess_path,
                cefAppBuilder.getJcefArgs()
        );
    }

    public void initialize() {
        synchronized (lock) {
            if (initialized) {
                return;
            }

            CefAppBuilder cefAppBuilder = GrapheneCefInstaller.createBuilder();
            logStartupConfiguration(cefAppBuilder);
            GrapheneCefAppHandler appHandler = new GrapheneCefAppHandler();
            cefAppBuilder.setAppHandler(appHandler);

            try {
                cefApp = cefAppBuilder.build();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Failed to initialize Graphene CEF runtime", exception);
            } catch (IOException | UnsupportedPlatformException | CefInitializationException exception) {
                throw new IllegalStateException("Failed to initialize Graphene CEF runtime", exception);
            }

            cefClient = cefApp.createClient();
            GrapheneCefClientConfig.configure(cefClient, loadEventBus, bridgeRuntime);
            remoteDebuggingPort = cefAppBuilder.getCefSettings().remote_debugging_port;
            initialized = true;

            registerShutdownHook();
            GrapheneCore.LOGGER.info("CEF runtime initialized on debug port {}", remoteDebuggingPort);
        }
    }

    public CefClient requireClient() {
        synchronized (lock) {
            if (!initialized || cefClient == null) {
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.init() first.");
            }

            return cefClient;
        }
    }

    public GrapheneLoadEventBus getLoadEventBus() {
        return loadEventBus;
    }

    public GrapheneBridge attachBridge(GrapheneBrowser browser) {
        synchronized (lock) {
            if (!initialized) {
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.init() first.");
            }

            return bridgeRuntime.attach(browser);
        }
    }

    public void detachBridge(GrapheneBrowser browser) {
        synchronized (lock) {
            bridgeRuntime.detach(browser);
        }
    }

    public int getRemoteDebuggingPort() {
        synchronized (lock) {
            return remoteDebuggingPort;
        }
    }

    public boolean isInitialized() {
        synchronized (lock) {
            return initialized;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            if (!initialized) {
                return;
            }

            surfaceManager.closeAll();
            bridgeRuntime.shutdown();
            loadEventBus.clear();

            if (cefClient != null) {
                cefClient.dispose();
            }

            if (cefApp != null) {
                cefApp.dispose();
                awaitCefTermination();
            }

            cefClient = null;
            cefApp = null;
            remoteDebuggingPort = -1;
            initialized = false;
            GrapheneCore.LOGGER.info("CEF runtime disposed");
        }
    }

    private void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(ignoredClient -> shutdown());
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "graphene-cef-shutdown"));
        shutdownHookRegistered = true;
    }
    private static void awaitCefTermination() {
        long deadlineNanos = System.nanoTime() + CEF_SHUTDOWN_TIMEOUT_NANOS;

        while (CefApp.getState() != CefApp.CefAppState.TERMINATED && System.nanoTime() < deadlineNanos) {
            try {
                Thread.sleep(CEF_SHUTDOWN_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                GrapheneCore.LOGGER.warn("Interrupted while waiting for CEF termination", exception);
                return;
            }
        }

        if (CefApp.getState() != CefApp.CefAppState.TERMINATED) {
            GrapheneCore.LOGGER.warn("Timed out while waiting for CEF termination; process may remain alive");
        }
    }
}
