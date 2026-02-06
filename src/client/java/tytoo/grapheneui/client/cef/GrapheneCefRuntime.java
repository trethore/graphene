package tytoo.grapheneui.client.cef;

import me.tytoo.jcefgithub.CefAppBuilder;
import me.tytoo.jcefgithub.CefInitializationException;
import me.tytoo.jcefgithub.UnsupportedPlatformException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.cef.CefApp;
import org.cef.CefClient;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.client.event.GrapheneLoadEventBus;

import java.io.IOException;

public final class GrapheneCefRuntime {
    private static final Object LOCK = new Object();
    private static final GrapheneLoadEventBus LOAD_EVENT_BUS = new GrapheneLoadEventBus();
    private static boolean initialized = false;
    private static boolean shutdownHookRegistered = false;
    private static CefApp cefApp;
    private static CefClient cefClient;
    private static int remoteDebuggingPort = -1;

    private GrapheneCefRuntime() {
    }

    public static void initialize() {
        synchronized (LOCK) {
            if (initialized) {
                return;
            }

            CefAppBuilder cefAppBuilder = GrapheneCefInstaller.createBuilder();
            GrapheneCefAppHandler appHandler = new GrapheneCefAppHandler();
            cefAppBuilder.setAppHandler(appHandler);

            try {
                cefApp = cefAppBuilder.build();
            } catch (IOException | UnsupportedPlatformException | InterruptedException |
                     CefInitializationException exception) {
                throw new IllegalStateException("Failed to initialize Graphene CEF runtime", exception);
            }

            cefClient = cefApp.createClient();
            GrapheneCefClientConfig.configure(cefClient, LOAD_EVENT_BUS);
            remoteDebuggingPort = cefAppBuilder.getCefSettings().remote_debugging_port;
            initialized = true;

            registerShutdownHook();
            GrapheneCore.LOGGER.info("CEF runtime initialized on debug port {}", remoteDebuggingPort);
        }
    }

    public static CefClient requireClient() {
        synchronized (LOCK) {
            if (!initialized || cefClient == null) {
                throw new IllegalStateException("Graphene is not initialized. Call GrapheneCore.init() first.");
            }

            return cefClient;
        }
    }

    public static GrapheneLoadEventBus getLoadEventBus() {
        return LOAD_EVENT_BUS;
    }

    public static int getRemoteDebuggingPort() {
        synchronized (LOCK) {
            return remoteDebuggingPort;
        }
    }

    public static boolean isInitialized() {
        synchronized (LOCK) {
            return initialized;
        }
    }

    public static void shutdown() {
        synchronized (LOCK) {
            if (!initialized) {
                return;
            }

            if (cefApp != null) {
                cefApp.dispose();
            }

            cefClient = null;
            cefApp = null;
            remoteDebuggingPort = -1;
            initialized = false;
            GrapheneCore.LOGGER.info("CEF runtime disposed");
        }
    }

    private static void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> GrapheneCefRuntime.shutdown());
        shutdownHookRegistered = true;
    }
}
