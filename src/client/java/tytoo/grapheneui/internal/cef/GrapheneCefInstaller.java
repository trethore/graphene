package tytoo.grapheneui.internal.cef;

import me.tytoo.jcefgithub.CefAppBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.GrapheneConfig;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class GrapheneCefInstaller {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefInstaller.class);
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefInstaller.class);

    private static final List<String> MIRRORS = List.of(
            "https://github.com/trethore/jcefgithub/releases/download/{mvn_version}/jcef-natives-{platform}-{tag}.jar"
    );

    private GrapheneCefInstaller() {
    }

    public static CefAppBuilder createBuilder(GrapheneConfig config) {
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");
        Path installPath = validatedConfig.jcefDownloadPath().toAbsolutePath().normalize();
        File installDir = installPath.toFile();

        CefAppBuilder cefAppBuilder = new CefAppBuilder();
        cefAppBuilder.setInstallDir(installDir);
        configureRuntimePaths(cefAppBuilder, installDir);
        cefAppBuilder.setMirrors(MIRRORS);
        cefAppBuilder.addJcefArgs("--remote-allow-origins=*");
        configureExtensionLoading(cefAppBuilder, validatedConfig);
        configurePlatformCompatibility(cefAppBuilder);

        try {
            Path cacheDirectory = Files.createDirectories(installPath.resolve("cache"));
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
                installDir.getAbsolutePath(),
                cefAppBuilder.getCefSettings().cache_path,
                cefAppBuilder.getCefSettings().remote_debugging_port
        );

        return cefAppBuilder;
    }

    private static void configureRuntimePaths(CefAppBuilder cefAppBuilder, File installDir) {
        String installPath = installDir.getAbsolutePath();

        if (GraphenePlatform.isMac()) {
            return;
        }

        cefAppBuilder.getCefSettings().resources_dir_path = installPath;
        cefAppBuilder.getCefSettings().locales_dir_path = installPath + File.separator + "locales";

        if (!GraphenePlatform.isLinux()) {
            return;
        }

        cefAppBuilder.getCefSettings().browser_subprocess_path = installPath + File.separator + "jcef_helper";
    }

    private static void configureExtensionLoading(CefAppBuilder cefAppBuilder, GrapheneConfig config) {
        Optional<Path> configuredExtensionFolder = config.extensionFolder();
        if (configuredExtensionFolder.isEmpty()) {
            cefAppBuilder.addJcefArgs("--disable-extensions");
            return;
        }

        Path extensionFolder = configuredExtensionFolder.orElseThrow().toAbsolutePath().normalize();
        List<Path> extensionDirectories = collectExtensionDirectories(extensionFolder);
        if (extensionDirectories.isEmpty()) {
            LOGGER.warn("No unpacked extensions found in {}, disabling extensions", extensionFolder);
            cefAppBuilder.addJcefArgs("--disable-extensions");
            return;
        }

        String extensionPaths = toExtensionArgument(extensionDirectories);
        cefAppBuilder.addJcefArgs("--disable-extensions-except=" + extensionPaths);
        cefAppBuilder.addJcefArgs("--load-extension=" + extensionPaths);
        LOGGER.info("Configured {} unpacked extension(s) from {}", extensionDirectories.size(), extensionFolder);
    }

    private static List<Path> collectExtensionDirectories(Path extensionFolder) {
        if (!Files.exists(extensionFolder)) {
            LOGGER.warn("Extension folder {} does not exist", extensionFolder);
            return List.of();
        }

        if (!Files.isDirectory(extensionFolder)) {
            LOGGER.warn("Extension folder {} is not a directory", extensionFolder);
            return List.of();
        }

        if (hasManifest(extensionFolder)) {
            return List.of(extensionFolder);
        }

        try (Stream<Path> children = Files.list(extensionFolder)) {
            return children
                    .filter(Files::isDirectory)
                    .filter(GrapheneCefInstaller::hasManifest)
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            LOGGER.warn("Failed to list extension folder {}", extensionFolder, exception);
            return List.of();
        }
    }

    private static boolean hasManifest(Path extensionDirectory) {
        return Files.isRegularFile(extensionDirectory.resolve("manifest.json"));
    }

    private static String toExtensionArgument(List<Path> extensionDirectories) {
        List<String> absolutePaths = extensionDirectories.stream()
                .map(path -> path.toAbsolutePath().normalize().toString())
                .toList();
        return String.join(",", absolutePaths);
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
