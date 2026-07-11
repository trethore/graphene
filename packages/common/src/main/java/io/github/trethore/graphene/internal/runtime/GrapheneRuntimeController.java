package io.github.trethore.graphene.internal.runtime;

import io.github.trethore.graphene.api.GrapheneBackend;
import io.github.trethore.graphene.api.GrapheneBackendRegistry;
import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.config.BrowserFileAccessPolicy;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.config.GrapheneHttpConfig;
import io.github.trethore.graphene.api.config.GrapheneRemoteDebugConfig;
import io.github.trethore.graphene.api.runtime.GrapheneHttpServer;
import io.github.trethore.graphene.api.runtime.GrapheneRuntime;
import io.github.trethore.graphene.api.runtime.GrapheneRuntimeState;
import io.github.trethore.graphene.api.url.AssetId;
import io.github.trethore.graphene.api.url.GrapheneClasspathUrls;
import io.github.trethore.graphene.internal.http.GrapheneHttpServerRuntime;
import io.github.trethore.graphene.internal.platform.GraphenePlatformServices;
import io.github.trethore.graphene.internal.url.GrapheneAppUrls;
import io.github.trethore.graphene.internal.url.GrapheneHttpUrls;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("java:S6548")
public final class GrapheneRuntimeController implements GrapheneRuntime, GrapheneBackend {
  private static final GrapheneRuntimeController INSTANCE = new GrapheneRuntimeController();

  private final Map<Class<?>, String> resolvedModIds = new IdentityHashMap<>();
  private final Map<String, GrapheneContext> contexts = new LinkedHashMap<>();
  private final Map<String, GrapheneConfig> configs = new LinkedHashMap<>();
  private final GrapheneHttpUrls httpUrls = new GrapheneHttpUrls(this::httpBaseUrl);
  private GraphenePlatformServices platformServices;
  private GrapheneBrowserRuntime browserRuntime = GrapheneBrowserRuntime.disabled();
  private GrapheneRuntimeState state = GrapheneRuntimeState.NEW;
  private GrapheneHttpServerRuntime httpServer = GrapheneHttpServerRuntime.disabled();
  private CompletableFuture<Void> initialization = new CompletableFuture<>();
  private CompletableFuture<Void> shutdown = CompletableFuture.completedFuture(null);
  private ExecutorService startupExecutor;
  private boolean registrationClosed;
  private boolean browserRuntimeInstalled;

  private GrapheneRuntimeController() {
    GrapheneBackendRegistry.install(this);
  }

  public static GrapheneRuntimeController instance() {
    return INSTANCE;
  }

  public synchronized void install(GraphenePlatformServices services) {
    Objects.requireNonNull(services, "services");
    if (platformServices != null) {
      throw new IllegalStateException("Graphene platform services are already installed");
    }

    platformServices = services;
    services.lifecycle().onStarted(this::startRegisteredConsumers);
    services.lifecycle().onStopping(this::shutdownAsync);
  }

  public synchronized void installBrowserRuntime(GrapheneBrowserRuntime installedBrowserRuntime) {
    Objects.requireNonNull(installedBrowserRuntime, "installedBrowserRuntime");
    if (browserRuntimeInstalled) {
      throw new IllegalStateException("Graphene browser runtime is already installed");
    }
    if (state != GrapheneRuntimeState.NEW) {
      throw new IllegalStateException("Graphene browser runtime must be installed before startup");
    }
    browserRuntime = installedBrowserRuntime;
    browserRuntimeInstalled = true;
  }

  public synchronized GrapheneContext register(Class<?> anchorClass, GrapheneConfig config) {
    requireRegistrationOpen();
    Class<?> validatedAnchorClass = Objects.requireNonNull(anchorClass, "anchorClass");
    String modId = resolvedModIds.get(validatedAnchorClass);
    if (modId == null) {
      try {
        modId = requirePlatformServices().modResolver().resolveModId(validatedAnchorClass);
      } catch (RuntimeException exception) {
        throw new IllegalArgumentException(
            "Failed to resolve Graphene consumer mod id for anchor class "
                + validatedAnchorClass.getName()
                + "; use Graphene.register(modId, config) when class resolution is unavailable",
            exception);
      }
      modId = normalizeModId(modId);
      resolvedModIds.put(validatedAnchorClass, modId);
    }
    return registerValidated(modId, config);
  }

  public synchronized GrapheneContext register(String modId, GrapheneConfig config) {
    requireRegistrationOpen();
    String validatedModId = normalizeModId(modId);
    if (!requirePlatformServices().modResolver().isModLoaded(validatedModId)) {
      throw new IllegalArgumentException(
          "No loaded mod with id " + validatedModId + " is available for Graphene registration");
    }
    return registerValidated(validatedModId, config);
  }

  public synchronized GrapheneContext context(Class<?> anchorClass) {
    Class<?> validatedAnchorClass = Objects.requireNonNull(anchorClass, "anchorClass");
    String modId = resolvedModIds.get(validatedAnchorClass);
    if (modId == null) {
      modId = normalizeModId(requirePlatformServices().modResolver().resolveModId(anchorClass));
      resolvedModIds.put(validatedAnchorClass, modId);
    }
    return context(modId);
  }

  public synchronized GrapheneContext context(String modId) {
    String validatedModId = normalizeModId(modId);
    GrapheneContext context = contexts.get(validatedModId);
    if (context == null) {
      throw new IllegalStateException(
          "No Graphene consumer registered for mod id " + validatedModId);
    }
    return context;
  }

  public synchronized GrapheneGlobalConfig globalConfig() {
    return mergeGlobalConfig();
  }

  @Override
  public GrapheneRuntime runtime() {
    return this;
  }

  public synchronized GrapheneHttpUrls httpUrls() {
    return httpUrls;
  }

  public synchronized void closeRegistration() {
    registrationClosed = true;
  }

  @Override
  public synchronized GrapheneRuntimeState state() {
    return state;
  }

  @Override
  public synchronized CompletionStage<Void> initializeAsync() {
    if (state == GrapheneRuntimeState.RUNNING || state == GrapheneRuntimeState.STARTING) {
      return initialization;
    }
    if (state != GrapheneRuntimeState.NEW) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Graphene cannot initialize from state " + state));
    }
    if (contexts.isEmpty()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("No Graphene consumer is registered"));
    }

    requirePlatformServices();
    registrationClosed = true;
    state = GrapheneRuntimeState.STARTING;
    initialization = new CompletableFuture<>();
    startupExecutor =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "Graphene Startup");
              thread.setDaemon(false);
              return thread;
            });
    startupExecutor.execute(this::initializeRuntime);
    return initialization;
  }

  @Override
  public void initialize() {
    try {
      initializeAsync().toCompletableFuture().join();
    } catch (CompletionException exception) {
      if (exception.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw exception;
    }
  }

  @Override
  public synchronized CompletionStage<Void> shutdownAsync() {
    if (state == GrapheneRuntimeState.STOPPED) {
      return shutdown;
    }
    if (state == GrapheneRuntimeState.STOPPING) {
      return shutdown;
    }
    if (state == GrapheneRuntimeState.NEW) {
      registrationClosed = true;
      state = GrapheneRuntimeState.STOPPED;
      shutdown = CompletableFuture.completedFuture(null);
      return shutdown;
    }

    state = GrapheneRuntimeState.STOPPING;
    shutdown = CompletableFuture.runAsync(this::shutdownRuntime);
    return shutdown;
  }

  @Override
  public synchronized boolean isInitialized() {
    return state == GrapheneRuntimeState.RUNNING;
  }

  @Override
  public synchronized OptionalInt remoteDebuggingPort() {
    if (state != GrapheneRuntimeState.RUNNING) {
      return OptionalInt.empty();
    }
    return browserRuntime.remoteDebuggingPort();
  }

  @Override
  public synchronized GrapheneHttpServer httpServer() {
    return httpServer;
  }

  private synchronized GrapheneContext registerValidated(String modId, GrapheneConfig config) {
    GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");
    GrapheneContext existingContext = contexts.get(modId);
    if (existingContext != null) {
      if (!Objects.equals(configs.get(modId), validatedConfig)) {
        throw new IllegalStateException(
            "Graphene consumer " + modId + " is already registered with a different config");
      }
      return existingContext;
    }

    GrapheneContext context =
        new GrapheneContext(
            modId,
            validatedConfig,
            GrapheneAppUrls.assets(modId),
            GrapheneClasspathUrls.assets(modId),
            httpUrls.assets(modId),
            path -> httpUrls.modUrl(modId, path),
            (url, options, width, height) ->
                browserRuntime.createSession(url, options, width, height));
    contexts.put(modId, context);
    configs.put(modId, validatedConfig);
    return context;
  }

  private void startRegisteredConsumers() {
    synchronized (this) {
      registrationClosed = true;
      if (contexts.isEmpty()) {
        return;
      }
    }
    initializeAsync();
  }

  private void initializeRuntime() {
    try {
      Map<String, GrapheneHttpConfig> httpConfigs = snapshotHttpConfigs();
      GrapheneHttpServerRuntime startedHttpServer =
          httpConfigs.isEmpty()
              ? GrapheneHttpServerRuntime.disabled()
              : GrapheneHttpServerRuntime.start(httpConfigs);
      initializeBrowserRuntime(startedHttpServer);
      synchronized (this) {
        httpServer = startedHttpServer;
        state = GrapheneRuntimeState.RUNNING;
        initialization.complete(null);
        closeStartupExecutor();
      }
    } catch (RuntimeException exception) {
      synchronized (this) {
        httpServer.close();
        httpServer = GrapheneHttpServerRuntime.disabled();
        state = GrapheneRuntimeState.FAILED;
        initialization.completeExceptionally(exception);
        closeStartupExecutor();
      }
    }
  }

  private void initializeBrowserRuntime(GrapheneHttpServerRuntime startedHttpServer) {
    try {
      browserRuntime.initialize(globalConfig());
    } catch (RuntimeException exception) {
      startedHttpServer.close();
      throw exception;
    }
  }

  private void shutdownRuntime() {
    RuntimeException shutdownFailure = null;
    synchronized (this) {
      httpServer.close();
      httpServer = GrapheneHttpServerRuntime.disabled();
      try {
        browserRuntime.shutdown();
      } catch (RuntimeException exception) {
        shutdownFailure = exception;
      }
      closeStartupExecutor();
      state = GrapheneRuntimeState.STOPPED;
    }
    if (shutdownFailure != null) {
      throw shutdownFailure;
    }
  }

  private synchronized Map<String, GrapheneHttpConfig> snapshotHttpConfigs() {
    LinkedHashMap<String, GrapheneHttpConfig> httpConfigs = new LinkedHashMap<>();
    configs.forEach(
        (consumerId, config) ->
            config.container().http().ifPresent(http -> httpConfigs.put(consumerId, http)));
    return Map.copyOf(httpConfigs);
  }

  private GrapheneGlobalConfig mergeGlobalConfig() {
    GrapheneGlobalConfig.Builder builder = GrapheneGlobalConfig.builder();
    OwnedValue<Path> runtimePath = null;
    OwnedValue<GrapheneRemoteDebugConfig> remoteDebug = null;
    BrowserFileAccessPolicy fileAccessPolicy = BrowserFileAccessPolicy.DENY;

    for (Map.Entry<String, GrapheneConfig> entry : configs.entrySet()) {
      String owner = entry.getKey();
      GrapheneGlobalConfig globalConfig = entry.getValue().global();
      runtimePath =
          mergeOwnedValue(
              runtimePath,
              globalConfig
                  .browserRuntimePath()
                  .map(path -> path.toAbsolutePath().normalize())
                  .orElse(null),
              owner,
              "browser runtime path");
      remoteDebug =
          mergeOwnedValue(
              remoteDebug,
              globalConfig.remoteDebugging().orElse(null),
              owner,
              "remote debugging config");
      globalConfig.extensionFolders().forEach(builder::extensionFolder);
      if (globalConfig.browserFileAccessPolicy() == BrowserFileAccessPolicy.ALLOW) {
        fileAccessPolicy = BrowserFileAccessPolicy.ALLOW;
      }
    }

    if (runtimePath != null) {
      builder.browserRuntimePath(runtimePath.value());
    }
    if (remoteDebug != null) {
      builder.remoteDebugging(remoteDebug.value());
    }
    builder.browserFileAccessPolicy(fileAccessPolicy);
    return builder.build();
  }

  private static <T> OwnedValue<T> mergeOwnedValue(
      OwnedValue<T> selected, T candidate, String owner, String setting) {
    if (candidate == null) {
      return selected;
    }
    if (selected == null) {
      return new OwnedValue<>(candidate, owner);
    }
    if (Objects.equals(selected.value(), candidate)) {
      return selected;
    }
    throw new IllegalStateException(
        "Conflicting Graphene "
            + setting
            + " between consumers "
            + selected.owner()
            + " and "
            + owner);
  }

  private static String normalizeModId(String modId) {
    return AssetId.normalizeNamespace(Objects.requireNonNull(modId, "modId").trim());
  }

  private GraphenePlatformServices requirePlatformServices() {
    if (platformServices == null) {
      throw new IllegalStateException(
          "Graphene platform services are not installed; the platform bootstrap must run first");
    }
    return platformServices;
  }

  private void requireRegistrationOpen() {
    if (registrationClosed) {
      throw new IllegalStateException("Graphene consumer registration is closed");
    }
  }

  private void closeStartupExecutor() {
    if (startupExecutor != null) {
      startupExecutor.shutdown();
      startupExecutor = null;
    }
  }

  private synchronized String httpBaseUrl() {
    return httpServer.isRunning() ? httpServer.baseUrl() : "";
  }

  private record OwnedValue<T>(T value, String owner) {}
}
