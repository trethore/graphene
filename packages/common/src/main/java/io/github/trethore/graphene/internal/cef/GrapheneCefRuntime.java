package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.internal.bridge.GrapheneBridgeOptions;
import io.github.trethore.graphene.internal.bridge.GrapheneBridgeRuntime;
import io.github.trethore.graphene.internal.event.GrapheneLoadEventBus;
import io.github.trethore.graphene.internal.platform.GrapheneStartupPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import io.github.trethore.graphene.internal.runtime.GrapheneBrowserRuntime;
import io.github.trethore.jcefgithub.CefAppBuilder;
import io.github.trethore.jcefgithub.CefInitializationException;
import io.github.trethore.jcefgithub.UnsupportedPlatformException;
import java.io.IOException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.cef.CefApp;
import org.cef.CefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneCefRuntime implements GrapheneBrowserRuntime {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefRuntime.class);
  private static final long SHUTDOWN_TIMEOUT = TimeUnit.SECONDS.toNanos(5);
  private static final long SHUTDOWN_POLL_INTERVAL = TimeUnit.MILLISECONDS.toNanos(25);

  private final GrapheneStartupPresenter startupPresenter;
  private final GrapheneTaskExecutor mainThreadExecutor;
  private final GrapheneBridgeRuntime bridgeRuntime;
  private final GrapheneLoadEventBus loadEventBus = new GrapheneLoadEventBus();
  private CefApp app;
  private CefClient client;
  private int remoteDebuggingPort = -1;

  public GrapheneCefRuntime(
      GrapheneStartupPresenter startupPresenter, GrapheneTaskExecutor mainThreadExecutor) {
    this.startupPresenter = Objects.requireNonNull(startupPresenter, "startupPresenter");
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    this.bridgeRuntime =
        new GrapheneBridgeRuntime(GrapheneBridgeOptions.defaults(), mainThreadExecutor);
  }

  @Override
  public synchronized void initialize(GrapheneGlobalConfig config) {
    if (app != null) {
      return;
    }
    CefAppBuilder builder = GrapheneCefInstaller.createBuilder(config);
    builder.setAppHandler(new GrapheneCefAppHandler(config.browserFileAccessPolicy()));
    builder.setProgressHandler(
        (state, percent) ->
            startupPresenter.update(state.name(), percent < 0 ? -1.0 : percent / 100.0));
    try {
      CefApp createdApp = buildApp(builder);
      CefClient createdClient;
      try {
        createdClient = createdApp.createClient();
        GrapheneCefClientConfig.configure(
            createdClient, loadEventBus, bridgeRuntime, mainThreadExecutor);
      } catch (RuntimeException exception) {
        disposePartial(null, createdApp);
        throw exception;
      }
      app = createdApp;
      client = createdClient;
      int configuredPort = builder.getCefSettings().remote_debugging_port;
      remoteDebuggingPort = configuredPort > 0 ? configuredPort : -1;
    } finally {
      startupPresenter.close();
    }
  }

  private static CefApp buildApp(CefAppBuilder builder) {
    try {
      return builder.build();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while initializing Graphene browser runtime", exception);
    } catch (IOException | UnsupportedPlatformException | CefInitializationException exception) {
      throw new IllegalStateException("Failed to initialize Graphene browser runtime", exception);
    }
  }

  @Override
  public synchronized void shutdown() {
    CefClient activeClient = client;
    CefApp activeApp = app;
    client = null;
    app = null;
    remoteDebuggingPort = -1;
    bridgeRuntime.shutdown();
    loadEventBus.clear();
    disposePartial(activeClient, activeApp);
  }

  @Override
  public synchronized OptionalInt remoteDebuggingPort() {
    return remoteDebuggingPort > 0 ? OptionalInt.of(remoteDebuggingPort) : OptionalInt.empty();
  }

  private static void disposePartial(CefClient activeClient, CefApp activeApp) {
    if (activeClient != null) {
      try {
        activeClient.dispose();
      } catch (RuntimeException exception) {
        LOGGER.warn("Failed to dispose JCEF client", exception);
      }
    }
    if (activeApp == null) {
      return;
    }
    try {
      activeApp.dispose();
      awaitTermination();
    } catch (RuntimeException exception) {
      LOGGER.warn("Failed to dispose JCEF application", exception);
    }
  }

  private static void awaitTermination() {
    long deadline = System.nanoTime() + SHUTDOWN_TIMEOUT;
    while (CefApp.getState() != CefApp.CefAppState.TERMINATED && System.nanoTime() < deadline) {
      LockSupport.parkNanos(SHUTDOWN_POLL_INTERVAL);
      if (Thread.currentThread().isInterrupted()) {
        return;
      }
    }
    if (CefApp.getState() != CefApp.CefAppState.TERMINATED) {
      LOGGER.warn("Timed out waiting for JCEF termination");
    }
  }
}
