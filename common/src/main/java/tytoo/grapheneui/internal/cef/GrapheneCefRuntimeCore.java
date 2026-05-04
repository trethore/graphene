package tytoo.grapheneui.internal.cef;

import io.github.trethore.jcefgithub.CefAppBuilder;
import io.github.trethore.jcefgithub.CefInitializationException;
import io.github.trethore.jcefgithub.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;
import tytoo.grapheneui.api.runtime.GrapheneHttpServer;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeBrowser;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeOptions;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.cef.startup.GrapheneCefStartupProgressHandler;
import tytoo.grapheneui.internal.cef.startup.GrapheneNativeDownloadState;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.event.GrapheneTitleEventBus;
import tytoo.grapheneui.internal.http.GrapheneHttpServerRuntime;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

public final class GrapheneCefRuntimeCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefRuntimeCore.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefRuntimeCore.class);
    private static final long CEF_SHUTDOWN_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long CEF_SHUTDOWN_POLL_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(25);
    private static final long BROWSER_CLOSE_TIMEOUT_MILLIS = 10_000L;
    private static final String FAILED_INITIALIZATION_MESSAGE = "Failed to initialize Graphene CEF runtime";
    private static final ExecutorService STARTUP_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "graphene-cef-startup");
        thread.setDaemon(true);
        return thread;
    });

    private final Object lock = new Object();
    private final RuntimeHooks hooks;
    private final GrapheneLoadEventBus loadEventBus = new GrapheneLoadEventBus();
    private final GrapheneTitleEventBus titleEventBus = new GrapheneTitleEventBus();
    private final GrapheneBridgeRuntime bridgeRuntime;
    private final GrapheneCefBrowserShutdownTracker browserShutdownTracker = new GrapheneCefBrowserShutdownTracker();
    private boolean initialized;
    private boolean shutdownInProgress;
    private CefApp cefApp;
    private CefClient cefClient;
    private int remoteDebuggingPort = -1;
    private GrapheneHttpServerRuntime httpServer = GrapheneHttpServerRuntime.disabled();
    private CompletableFuture<Void> initializationFuture;

    public GrapheneCefRuntimeCore(RuntimeHooks hooks) {
        this(hooks, GrapheneBridgeOptions.defaults());
    }

    public GrapheneCefRuntimeCore(RuntimeHooks hooks, GrapheneBridgeOptions bridgeOptions) {
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.bridgeRuntime = new GrapheneBridgeRuntime(
                Objects.requireNonNull(bridgeOptions, "bridgeOptions"),
                this.hooks.mainThreadExecutor()
        );
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

    private static int browserIdentifier(GrapheneBridgeBrowser browser) {
        try {
            return browser.getIdentifier();
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static int browserIdentifier(CefBrowser browser) {
        try {
            return browser.getIdentifier();
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    public void initialize(GrapheneGlobalConfig globalConfig, Map<String, GrapheneContainerConfig> containerConfigs) {
        try {
            initializeAsync(globalConfig, containerConfigs).join();
        } catch (CompletionException exception) {
            throw propagateInitializationFailure(exception.getCause());
        }
    }

    public CompletableFuture<Void> initializeAsync(
            GrapheneGlobalConfig globalConfig,
            Map<String, GrapheneContainerConfig> containerConfigs
    ) {
        GrapheneGlobalConfig validatedGlobalConfig = Objects.requireNonNull(globalConfig, "globalConfig");
        Map<String, GrapheneContainerConfig> validatedContainerConfigs = Map.copyOf(
                Objects.requireNonNull(containerConfigs, "containerConfigs")
        );
        synchronized (lock) {
            if (initialized) {
                return CompletableFuture.completedFuture(null);
            }

            if (initializationFuture != null) {
                return initializationFuture;
            }

            if (shutdownInProgress) {
                return CompletableFuture.failedFuture(new IllegalStateException("Graphene CEF runtime is shutting down"));
            }

            CompletableFuture<Void> startupFuture = CompletableFuture.runAsync(
                    () -> initializeInternal(validatedGlobalConfig, validatedContainerConfigs),
                    STARTUP_EXECUTOR
            );
            initializationFuture = startupFuture;
            startupFuture.whenComplete((ignored, throwable) -> {
                synchronized (lock) {
                    if (initializationFuture == startupFuture) {
                        initializationFuture = null;
                    }
                }

                if (throwable != null) {
                    LOGGER.error("Failed to initialize Graphene CEF runtime asynchronously", unwrapInitializationFailure(throwable));
                }
            });
            return startupFuture;
        }
    }

    public CefClient requireClient() {
        synchronized (lock) {
            if (!initialized || cefClient == null) {
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.register(...) first.");
            }

            return cefClient;
        }
    }

    public GrapheneLoadEventBus getLoadEventBus() {
        return loadEventBus;
    }

    public GrapheneTitleEventBus getTitleEventBus() {
        return titleEventBus;
    }

    public GrapheneBridge attachBridge(GrapheneBridgeBrowser browser) {
        synchronized (lock) {
            if (!initialized) {
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.register(...) first.");
            }

            GrapheneBridge bridge = bridgeRuntime.attach(browser);
            DEBUG_LOGGER.debug("Attached bridge for browser identifier={}", browserIdentifier(browser));
            return bridge;
        }
    }

    public void detachBridge(CefBrowser browser) {
        synchronized (lock) {
            bridgeRuntime.detach(browser);
            DEBUG_LOGGER.debug("Detached bridge for browser identifier={}", browserIdentifier(browser));
        }
    }

    public void onNavigationRequested(CefBrowser browser) {
        synchronized (lock) {
            if (!initialized) {
                return;
            }

            bridgeRuntime.onNavigationRequested(browser);
            DEBUG_LOGGER.debug("Bridge navigation requested for browser identifier={}", browserIdentifier(browser));
        }
    }

    public void ensureBootstrap(CefBrowser browser) {
        synchronized (lock) {
            if (!initialized) {
                return;
            }

            bridgeRuntime.ensureBootstrap(browser);
            DEBUG_LOGGER.debug("Requested bridge bootstrap check for browser identifier={}", browserIdentifier(browser));
        }
    }

    public int getRemoteDebuggingPort() {
        synchronized (lock) {
            return remoteDebuggingPort;
        }
    }

    public GrapheneHttpServer httpServer() {
        synchronized (lock) {
            return httpServer;
        }
    }

    public boolean isInitialized() {
        synchronized (lock) {
            return initialized;
        }
    }

    public void shutdown() {
        shutdownInternal();
    }

    private boolean ensureCanInitialize() {
        if (initialized) {
            DEBUG_LOGGER.debug("Skipping CEF initialize because runtime is already initialized");
            return false;
        }

        if (shutdownInProgress) {
            throw new IllegalStateException("Graphene CEF runtime is shutting down");
        }

        return true;
    }

    private void initializeInternal(
            GrapheneGlobalConfig globalConfig,
            Map<String, GrapheneContainerConfig> containerConfigs
    ) {
        GrapheneNativeDownloadState downloadState = new GrapheneNativeDownloadState(GrapheneCefInstaller.currentPlatformIdentifier());

        try {
            synchronized (lock) {
                if (!ensureCanInitialize()) {
                    return;
                }

                GrapheneHttpServerRuntime startedHttpServer = createHttpServerIfConfigured(containerConfigs);
                CefAppBuilder cefAppBuilder = createConfiguredBuilder(globalConfig, downloadState);
                buildCefApp(cefAppBuilder, startedHttpServer);
                initializeClient(cefAppBuilder, startedHttpServer);
                logInitializationState();
            }
        } finally {
            hooks.dismissStartupProgress(downloadState);
        }
    }

    private CefAppBuilder createConfiguredBuilder(
            GrapheneGlobalConfig globalConfig,
            GrapheneNativeDownloadState downloadState
    ) {
        CefAppBuilder cefAppBuilder = GrapheneCefInstaller.createBuilder(globalConfig);
        cefAppBuilder.setProgressHandler(new GrapheneCefStartupProgressHandler(
                downloadState,
                () -> hooks.showStartupProgress(downloadState)
        ));
        logStartupConfiguration(cefAppBuilder);
        hooks.configureCefApp(cefAppBuilder, globalConfig);
        return cefAppBuilder;
    }

    private IllegalStateException propagateInitializationFailure(Throwable throwable) {
        Throwable cause = unwrapInitializationFailure(throwable);
        if (cause instanceof IllegalStateException illegalStateException) {
            return illegalStateException;
        }

        return new IllegalStateException(FAILED_INITIALIZATION_MESSAGE, cause);
    }

    private Throwable unwrapInitializationFailure(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException completionException && completionException.getCause() != null) {
            cause = completionException.getCause();
        }

        return cause == null ? new IllegalStateException(FAILED_INITIALIZATION_MESSAGE) : cause;
    }

    private void buildCefApp(CefAppBuilder cefAppBuilder, GrapheneHttpServerRuntime startedHttpServer) {
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
    }

    private void initializeClient(CefAppBuilder cefAppBuilder, GrapheneHttpServerRuntime startedHttpServer) {
        try {
            cefClient = cefApp.createClient();
            hooks.configureCefClient(
                    cefClient,
                    loadEventBus,
                    titleEventBus,
                    bridgeRuntime,
                    browserShutdownTracker
            );
            int configuredRemoteDebugPort = cefAppBuilder.getCefSettings().remote_debugging_port;
            remoteDebuggingPort = configuredRemoteDebugPort > 0 ? configuredRemoteDebugPort : -1;
            httpServer = startedHttpServer;
            initialized = true;
        } catch (RuntimeException exception) {
            startedHttpServer.close();
            resetInitializationStateAfterFailure();
            throw exception;
        }
    }

    private void resetInitializationStateAfterFailure() {
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
    }

    private void logInitializationState() {
        if (remoteDebuggingPort > 0) {
            LOGGER.info("CEF runtime initialized on debug port {}", remoteDebuggingPort);
        } else {
            LOGGER.info("CEF runtime initialized with remote debugging disabled");
        }

        if (httpServer.isRunning()) {
            LOGGER.info("Graphene HTTP server initialized at {}", httpServer.baseUrl());
        }
    }

    private GrapheneHttpServerRuntime createHttpServerIfConfigured(Map<String, GrapheneContainerConfig> containerConfigs) {
        LinkedHashMap<String, GrapheneHttpConfig> httpConfigs = new LinkedHashMap<>();
        for (Map.Entry<String, GrapheneContainerConfig> containerConfigEntry : containerConfigs.entrySet()) {
            containerConfigEntry.getValue().http().ifPresent(httpConfig -> httpConfigs.put(containerConfigEntry.getKey(), httpConfig));
        }

        if (httpConfigs.isEmpty()) {
            return GrapheneHttpServerRuntime.disabled();
        }

        return createHttpServer(httpConfigs);
    }

    private GrapheneHttpServerRuntime createHttpServer(Map<String, GrapheneHttpConfig> httpConfigs) {
        GrapheneHttpServerRuntime startedHttpServer = GrapheneHttpServerRuntime.start(httpConfigs);
        DEBUG_LOGGER.debug(
                "Graphene HTTP server started: host={}, port={}, mountCount={}",
                startedHttpServer.host(),
                startedHttpServer.port(),
                httpConfigs.size()
        );
        return startedHttpServer;
    }

    private void shutdownInternal() {
        ShutdownResources resources = beginShutdown();
        if (resources == null) {
            return;
        }

        try {
            closeSurfaces();
            runShutdownStep(bridgeRuntime::shutdown, "Failed to shut down Graphene bridge runtime");
            runShutdownStep(loadEventBus::clear, "Failed to clear Graphene load event listeners");
            runShutdownStep(titleEventBus::clear, "Failed to clear Graphene title event listeners");
            disposeNativeResources(resources);
            LOGGER.info("CEF runtime disposed (client lifecycle)");
        } finally {
            synchronized (lock) {
                shutdownInProgress = false;
            }
        }
    }

    private ShutdownResources beginShutdown() {
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

            DEBUG_LOGGER.debug("Starting CEF shutdown closeClient={} closeApp={} httpRunning={}",
                    resources.cefClient() != null,
                    resources.cefApp() != null,
                    resources.httpServerRunning()
            );

            return resources;
        }
    }

    private void disposeNativeResources(ShutdownResources resources) {
        CefClient activeClient = resources.cefClient();
        if (activeClient != null) {
            runShutdownStep(activeClient::dispose, "Failed to dispose CEF client");
            awaitBrowserCloseCallbacks();
        }

        CefApp activeApp = resources.cefApp();
        if (activeApp != null) {
            runShutdownStep(() -> disposeCefApp(activeApp), "Failed to dispose CEF app");
        }

        runShutdownStep(resources.closeHttpServer(), "Failed to stop Graphene HTTP server");
    }

    private void closeSurfaces() {
        runShutdownStep(hooks::closeSurfaces, "Failed to close browser surfaces during CEF shutdown");
    }

    private void disposeCefApp(CefApp activeApp) {
        activeApp.dispose();
        awaitCefTermination();
    }

    private void awaitCefTermination() {
        if (hooks.isMainThread()) {
            DEBUG_LOGGER.debug("Skipping blocking CEF termination wait on the Minecraft main thread");
            return;
        }

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

    private void awaitBrowserCloseCallbacks() {
        if (hooks.isMainThread()) {
            DEBUG_LOGGER.debug(
                    "Skipping blocking CEF browser close wait on the Minecraft main thread; openBrowserCount={}",
                    browserShutdownTracker.openBrowserCount()
            );
            return;
        }

        try {
            browserShutdownTracker.allBrowsersClosedFuture().get(BROWSER_CLOSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for CEF browsers to close");
        } catch (ExecutionException | TimeoutException exception) {
            LOGGER.warn(
                    "Timed out while waiting for {} CEF browser(s) to close",
                    browserShutdownTracker.openBrowserCount(),
                    exception
            );
        }
    }

    private void runShutdownStep(Runnable action, String failureMessage) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            LOGGER.warn(failureMessage, exception);
        }
    }

    private record ShutdownResources(
            CefClient cefClient,
            CefApp cefApp,
            Runnable closeHttpServer,
            boolean httpServerRunning
    ) {
    }

    public interface RuntimeHooks {
        void configureCefApp(CefAppBuilder cefAppBuilder, GrapheneGlobalConfig globalConfig);

        void configureCefClient(
                CefClient cefClient,
                GrapheneLoadEventBus loadEventBus,
                GrapheneTitleEventBus titleEventBus,
                GrapheneBridgeRuntime bridgeRuntime,
                GrapheneCefBrowserShutdownTracker browserShutdownTracker
        );

        void showStartupProgress(GrapheneNativeDownloadState downloadState);

        void dismissStartupProgress(GrapheneNativeDownloadState downloadState);

        void closeSurfaces();

        boolean isMainThread();

        tytoo.grapheneui.internal.core.GrapheneMainThreadExecutor mainThreadExecutor();
    }
}
