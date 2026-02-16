package tytoo.grapheneui.internal.cef;

import me.tytoo.jcefgithub.CefAppBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GrapheneCefInstaller {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefInstaller.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefInstaller.class);

    private static final File INSTALLATION_DIR = new File("./jcef");
    private static final List<String> MIRRORS = List.of(
            "https://github.com/trethore/jcefgithub/releases/download/{mvn_version}/jcef-natives-{platform}-{tag}.jar"
    );

    private GrapheneCefInstaller() {
    }

    public static CefAppBuilder createBuilder() {
        CefAppBuilder cefAppBuilder = new CefAppBuilder();
        cefAppBuilder.setInstallDir(INSTALLATION_DIR);
        configureRuntimePaths(cefAppBuilder);
        cefAppBuilder.setMirrors(MIRRORS);
        cefAppBuilder.addJcefArgs("--remote-allow-origins=*");
        configurePlatformCompatibility(cefAppBuilder);

        try {
            Path cacheDirectory = Files.createDirectories(INSTALLATION_DIR.toPath().resolve("cache"));
            String cachePath = cacheDirectory.toAbsolutePath().toString();
            cefAppBuilder.getCefSettings().cache_path = cachePath;
            cefAppBuilder.getCefSettings().root_cache_path = cachePath;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create Graphene CEF cache directory", exception);
        }

        cefAppBuilder.getCefSettings().windowless_rendering_enabled = true;
        cefAppBuilder.getCefSettings().remote_debugging_port = findRandomPort();

        DEBUG_LOGGER.debug(
                "Configured CEF installDir={} cachePath={} remoteDebugPort={}",
                INSTALLATION_DIR.getAbsolutePath(),
                cefAppBuilder.getCefSettings().cache_path,
                cefAppBuilder.getCefSettings().remote_debugging_port
        );

        return cefAppBuilder;
    }

    private static void configureRuntimePaths(CefAppBuilder cefAppBuilder) {
        String installPath = INSTALLATION_DIR.getAbsolutePath();
        cefAppBuilder.getCefSettings().resources_dir_path = installPath;
        cefAppBuilder.getCefSettings().locales_dir_path = installPath + File.separator + "locales";

        if (!GraphenePlatform.isLinux()) {
            return;
        }

        cefAppBuilder.getCefSettings().browser_subprocess_path = installPath + File.separator + "jcef_helper";
    }

    private static void configurePlatformCompatibility(CefAppBuilder cefAppBuilder) {
        if (!GraphenePlatform.isLinux()) {
            return;
        }

        cefAppBuilder.addJcefArgs(
                "--no-sandbox",
                "--password-store=basic",
                "--disable-background-networking",
                "--disable-component-update",
                "--disable-domain-reliability",
                "--disable-sync",
                "--disable-extensions",
                "--metrics-recording-only",
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-features=MediaRouter,OptimizationHints,AutofillServerCommunication,CertificateTransparencyComponentUpdater,Translate"
        );

        if (!GraphenePlatform.isWaylandSession()) {
            return;
        }

        cefAppBuilder.addJcefArgs("--ozone-platform=x11");
        LOGGER.info("Detected Wayland session, forcing CEF to X11 compatibility mode");
    }

    private static int findRandomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException _) {
            return 9222;
        }
    }
}
