package tytoo.grapheneui.internal.cef;

import me.tytoo.jcefgithub.CefAppBuilder;
import me.tytoo.jcefgithub.CefInitializationException;
import me.tytoo.jcefgithub.UnsupportedPlatformException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.cef.CefApp;
import org.cef.CefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.GrapheneConfig;
import tytoo.grapheneui.api.GrapheneHttpConfig;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeOptions;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.browser.GrapheneBrowserSurfaceManager;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.http.GrapheneHttpServerRuntime;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class GrapheneCefRuntime implements GrapheneRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefRuntime.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefRuntime.class);

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

            GrapheneHttpServerRuntime startedHttpServer = createHttpServerIfConfigured(validatedConfig);

            CefAppBuilder cefAppBuilder = GrapheneCefInstaller.createBuilder(validatedConfig);
            logStartupConfiguration(cefAppBuilder);
            GrapheneCefAppHandler appHandler = new GrapheneCefAppHandler();
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
                remoteDebuggingPort = cefAppBuilder.getCefSettings().remote_debugging_port;
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
            LOGGER.info("CEF runtime initialized on debug port {}", remoteDebuggingPort);
            if (httpServer.isRunning()) {
                LOGGER.info("Graphene HTTP server initialized at {}", httpServer.baseUrl());
            }
        }
    }

    public CefClient requireClient() {
        synchronized (lock) {
            if (!initialized || cefClient == null) {
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.init(modId) first.");
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
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.init(modId) first.");
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
        synchronized (lock) {
            if (!initialized) {
                DEBUG_LOGGER.debug("Skipping CEF shutdown because runtime is not initialized");
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
            httpServer.close();
            httpServer = GrapheneHttpServerRuntime.disabled();
            remoteDebuggingPort = -1;
            initialized = false;
            LOGGER.info("CEF runtime disposed");
        }
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
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "graphene-cef-shutdown"));
        shutdownHookRegistered = true;
        DEBUG_LOGGER.debug("Registered CEF shutdown hooks");
    }
}
