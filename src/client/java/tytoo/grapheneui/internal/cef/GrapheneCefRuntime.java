package tytoo.grapheneui.internal.cef;

import io.github.trethore.jcefgithub.CefAppBuilder;
import io.github.trethore.jcefgithub.CefInitializationException;
import io.github.trethore.jcefgithub.UnsupportedPlatformException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.cef.CefApp;
import org.cef.CefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeOptions;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.browser.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.http.GrapheneHttpServerRuntime;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.mc.McClient;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

public final class GrapheneCefRuntime implements GrapheneRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefRuntime.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefRuntime.class);

    private static final long CEF_SHUTDOWN_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long CEF_SHUTDOWN_POLL_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(25);
    private static final long SHUTDOWN_HOOK_MAIN_THREAD_TIMEOUT_MILLIS = 2_000L;
    private static final String FAILED_INITIALIZATION_MESSAGE = "Failed to initialize Graphene CEF runtime";

    private final Object lock = new Object();
    private final GrapheneBrowserSurfaceManager surfaceManager;
    private final GrapheneLoadEventBus loadEventBus = new GrapheneLoadEventBus();
    private final GrapheneBridgeRuntime bridgeRuntime;
    private boolean initialized;
    private boolean shutdownInProgress;
    private boolean shutdownHookRegistered;
    private CefApp cefApp;
    private CefClient cefClient;
    private int remoteDebuggingPort = -1;
    private GrapheneHttpServerRuntime httpServer = GrapheneHttpServerRuntime.disabled();

    public GrapheneCefRuntime(GrapheneBrowserSurfaceManager surfaceManager) {
        this(surfaceManager, GrapheneBridgeOptions.defaults());
    }

    public GrapheneCefRuntime(GrapheneBrowserSurfaceManager surfaceManager, GrapheneBridgeOptions bridgeOptions) {
        this.surfaceManager = Objects.requireNonNull(surfaceManager, "surfaceManager");
        this.bridgeRuntime = new GrapheneBridgeRuntime(Objects.requireNonNull(bridgeOptions, "bridgeOptions"));
    }

    private static void logStartupConfiguration(CefAppBuilder cefAppBuilder) {
        LOGGER.info(
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
                LOGGER.warn("Interrupted while waiting for CEF termination");
                return;
            }
        }

        if (CefApp.getState() != CefApp.CefAppState.TERMINATED) {
            LOGGER.warn("Timed out while waiting for CEF termination; process may remain alive");
        }
    }

    private static int browserIdentifier(GrapheneBrowser browser) {
        try {
            return browser.getIdentifier();
        } catch (RuntimeException ignored) {
            // Browser may already be disposed while diagnostics are logged.
            return -1;
        }
    }

    public void initialize() {
        initialize(GrapheneConfig.defaults());
    }

    public void initialize(GrapheneConfig config) {
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");
        synchronized (lock) {
            if (initialized) {
                DEBUG_LOGGER.debug("Skipping CEF initialize because runtime is already initialized");
                return;
            }

            if (shutdownInProgress) {
                throw new IllegalStateException("Graphene CEF runtime is shutting down");
            }

            GrapheneHttpServerRuntime startedHttpServer = createHttpServerIfConfigured(validatedConfig);

            CefAppBuilder cefAppBuilder = GrapheneCefInstaller.createBuilder(validatedConfig);
            logStartupConfiguration(cefAppBuilder);
            GrapheneCefAppHandler appHandler = new GrapheneCefAppHandler(validatedConfig.fileSystemAccessMode());
            cefAppBuilder.setAppHandler(appHandler);

            try {
                cefApp = cefAppBuilder.build();
            } catch (InterruptedException exception) {
                startedHttpServer.close();
                Thread.currentThread().interrupt();
                throw new IllegalStateException(FAILED_INITIALIZATION_MESSAGE, exception);
            } catch (IOException | UnsupportedPlatformException | CefInitializationException exception) {
                startedHttpServer.close();
                throw new IllegalStateException(FAILED_INITIALIZATION_MESSAGE, exception);
            } catch (RuntimeException exception) {
                startedHttpServer.close();
                throw exception;
            }

            try {
                cefClient = cefApp.createClient();
                GrapheneCefClientConfig.configure(cefClient, loadEventBus, bridgeRuntime);
                int configuredRemoteDebugPort = cefAppBuilder.getCefSettings().remote_debugging_port;
                remoteDebuggingPort = configuredRemoteDebugPort > 0 ? configuredRemoteDebugPort : -1;
                httpServer = startedHttpServer;
                initialized = true;
            } catch (RuntimeException exception) {
                startedHttpServer.close();
                if (cefClient != null) {
                    cefClient.dispose();
                }

                cefApp.dispose();
                awaitCefTermination();

                cefClient = null;
                cefApp = null;
                httpServer = GrapheneHttpServerRuntime.disabled();
                remoteDebuggingPort = -1;
                initialized = false;
                throw exception;
            }

            registerShutdownHook();
            if (remoteDebuggingPort > 0) {
                LOGGER.info("CEF runtime initialized on debug port {}", remoteDebuggingPort);
            } else {
                LOGGER.info("CEF runtime initialized with remote debugging disabled");
            }
            if (httpServer.isRunning()) {
                LOGGER.info("Graphene HTTP server initialized at {}", httpServer.baseUrl());
            }
        }
    }

    public CefClient requireClient() {
        synchronized (lock) {
            if (!initialized || cefClient == null) {
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.register(modId) first.");
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
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.register(modId) first.");
            }

            GrapheneBridge bridge = bridgeRuntime.attach(browser);
            DEBUG_LOGGER.debug("Attached bridge for browser identifier={}", browserIdentifier(browser));
            return bridge;
        }
    }

    public void detachBridge(GrapheneBrowser browser) {
        synchronized (lock) {
            bridgeRuntime.detach(browser);
            DEBUG_LOGGER.debug("Detached bridge for browser identifier={}", browserIdentifier(browser));
        }
    }

    public void onNavigationRequested(GrapheneBrowser browser) {
        synchronized (lock) {
            if (!initialized) {
                return;
            }

            bridgeRuntime.onNavigationRequested(browser);
            DEBUG_LOGGER.debug("Bridge navigation requested for browser identifier={}", browserIdentifier(browser));
        }
    }

    public void ensureBootstrap(GrapheneBrowser browser) {
        synchronized (lock) {
            if (!initialized) {
                return;
            }

            bridgeRuntime.ensureBootstrap(browser);
            DEBUG_LOGGER.debug("Requested bridge bootstrap check for browser identifier={}", browserIdentifier(browser));
        }
    }

    @Override
    public int getRemoteDebuggingPort() {
        synchronized (lock) {
            return remoteDebuggingPort;
        }
    }

    @Override
    public GrapheneHttpServer httpServer() {
        synchronized (lock) {
            return httpServer;
        }
    }

    @Override
    public boolean isInitialized() {
        synchronized (lock) {
            return initialized;
        }
    }

    public void shutdown() {
        shutdownInternal(true, "client lifecycle");
    }

    private GrapheneHttpServerRuntime createHttpServerIfConfigured(GrapheneConfig config) {
        return config.http()
                .map(this::createHttpServer)
                .orElseGet(GrapheneHttpServerRuntime::disabled);
    }

    private GrapheneHttpServerRuntime createHttpServer(GrapheneHttpConfig httpConfig) {
        GrapheneHttpServerRuntime startedHttpServer = GrapheneHttpServerRuntime.start(httpConfig);
        DEBUG_LOGGER.debug(
                "Graphene HTTP server started: host={}, port={}, fileRoot={}, fallback={}",
                startedHttpServer.host(),
                startedHttpServer.port(),
                startedHttpServer.fileRoot(),
                startedHttpServer.spaFallbackResourcePath()
        );
        return startedHttpServer;
    }

    private void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(ignoredClient -> shutdown());
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownFromHook, "graphene-cef-shutdown"));
        shutdownHookRegistered = true;
        DEBUG_LOGGER.debug("Registered CEF shutdown hooks");
    }

    private void shutdownFromHook() {
        if (tryShutdownOnMainThreadFromHook()) {
            return;
        }

        LOGGER.warn("Falling back to direct CEF shutdown from JVM shutdown hook");
        shutdownInternal(false, "jvm shutdown hook fallback");
    }

    private boolean tryShutdownOnMainThreadFromHook() {
        synchronized (lock) {
            if (!initialized && !shutdownInProgress) {
                return true;
            }
        }

        try {
            CompletableFuture<Void> mainThreadShutdown = McClient.supplyOnMainThread(() -> {
                shutdown();
                return null;
            });
            mainThreadShutdown.get(SHUTDOWN_HOOK_MAIN_THREAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for main-thread CEF shutdown", exception);
            return false;
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            LOGGER.warn("Failed to run CEF shutdown on the Minecraft main thread", exception);
            return false;
        }
    }

    private void shutdownInternal(boolean closeSurfaces, String trigger) {
        ShutdownResources resources = beginShutdown(trigger);
        if (resources == null) {
            return;
        }

        try {
            if (closeSurfaces) {
                try {
                    surfaceManager.closeAll();
                } catch (RuntimeException exception) {
                    LOGGER.warn("Failed to close browser surfaces during CEF shutdown", exception);
                }
            }

            try {
                bridgeRuntime.shutdown();
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to shut down Graphene bridge runtime", exception);
            }

            try {
                loadEventBus.clear();
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to clear Graphene load event listeners", exception);
            }

            disposeNativeResources(resources);
            LOGGER.info("CEF runtime disposed ({})", trigger);
        } finally {
            synchronized (lock) {
                shutdownInProgress = false;
            }
        }
    }

    private ShutdownResources beginShutdown(String trigger) {
        synchronized (lock) {
            if (shutdownInProgress) {
                DEBUG_LOGGER.debug("Skipping CEF shutdown because another shutdown is already in progress");
                return null;
            }

            if (!initialized) {
                DEBUG_LOGGER.debug("Skipping CEF shutdown because runtime is not initialized");
                return null;
            }

            shutdownInProgress = true;

            GrapheneHttpServerRuntime activeHttpServer = httpServer;
            ShutdownResources resources = new ShutdownResources(
                    cefClient,
                    cefApp,
                    activeHttpServer::close,
                    activeHttpServer.isRunning()
            );
            cefClient = null;
            cefApp = null;
            httpServer = GrapheneHttpServerRuntime.disabled();
            remoteDebuggingPort = -1;
            initialized = false;

            DEBUG_LOGGER.debug("Starting CEF shutdown trigger={} closeClient={} closeApp={} httpRunning={}",
                    trigger,
                    resources.cefClient() != null,
                    resources.cefApp() != null,
                    resources.httpServerRunning()
            );

            return resources;
        }
    }

    private void disposeNativeResources(ShutdownResources resources) {
        try {
            if (resources.cefClient() != null) {
                resources.cefClient().dispose();
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to dispose CEF client", exception);
        }

        try {
            if (resources.cefApp() != null) {
                resources.cefApp().dispose();
                awaitCefTermination();
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to dispose CEF app", exception);
        }

        try {
            resources.closeHttpServer().run();
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to stop Graphene HTTP server", exception);
        }
    }

    private record ShutdownResources(
            CefClient cefClient,
            CefApp cefApp,
            Runnable closeHttpServer,
            boolean httpServerRunning
    ) {
    }
}
