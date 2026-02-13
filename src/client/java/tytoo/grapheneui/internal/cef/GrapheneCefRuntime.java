package tytoo.grapheneui.internal.cef;

import me.tytoo.jcefgithub.CefAppBuilder;
import me.tytoo.jcefgithub.CefInitializationException;
import me.tytoo.jcefgithub.UnsupportedPlatformException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.cef.CefApp;
import org.cef.CefClient;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeOptions;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.browser.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class GrapheneCefRuntime implements GrapheneRuntime {
    private static final long CEF_SHUTDOWN_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long CEF_SHUTDOWN_POLL_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(25);
    private static final String FAILED_INITIALIZATION_MESSAGE = "Failed to initialize Graphene CEF runtime";

    private final Object lock = new Object();
    private final GrapheneBrowserSurfaceManager surfaceManager;
    private final GrapheneLoadEventBus loadEventBus = new GrapheneLoadEventBus();
    private final GrapheneBridgeRuntime bridgeRuntime;
    private boolean initialized;
    private boolean shutdownHookRegistered;
    private CefApp cefApp;
    private CefClient cefClient;
    private int remoteDebuggingPort = -1;

    public GrapheneCefRuntime(GrapheneBrowserSurfaceManager surfaceManager) {
        this(surfaceManager, GrapheneBridgeOptions.defaults());
    }

    public GrapheneCefRuntime(GrapheneBrowserSurfaceManager surfaceManager, GrapheneBridgeOptions bridgeOptions) {
        this.surfaceManager = Objects.requireNonNull(surfaceManager, "surfaceManager");
        this.bridgeRuntime = new GrapheneBridgeRuntime(Objects.requireNonNull(bridgeOptions, "bridgeOptions"));
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

    private static void awaitCefTermination() {
        long deadlineNanos = System.nanoTime() + CEF_SHUTDOWN_TIMEOUT_NANOS;

        while (CefApp.getState() != CefApp.CefAppState.TERMINATED && System.nanoTime() < deadlineNanos) {
            LockSupport.parkNanos(CEF_SHUTDOWN_POLL_INTERVAL_NANOS);
            if (Thread.currentThread().isInterrupted()) {
                GrapheneCore.LOGGER.warn("Interrupted while waiting for CEF termination");
                return;
            }
        }

        if (CefApp.getState() != CefApp.CefAppState.TERMINATED) {
            GrapheneCore.LOGGER.warn("Timed out while waiting for CEF termination; process may remain alive");
        }
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
                throw new IllegalStateException(FAILED_INITIALIZATION_MESSAGE, exception);
            } catch (IOException | UnsupportedPlatformException | CefInitializationException exception) {
                throw new IllegalStateException(FAILED_INITIALIZATION_MESSAGE, exception);
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

    @Override
    public int getRemoteDebuggingPort() {
        synchronized (lock) {
            return remoteDebuggingPort;
        }
    }

    @Override
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
}
