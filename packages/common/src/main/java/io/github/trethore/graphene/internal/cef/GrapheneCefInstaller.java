package io.github.trethore.graphene.internal.cef;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.config.GrapheneRemoteDebugConfig;
import io.github.trethore.jcefgithub.CefAppBuilder;
import io.github.trethore.jcefgithub.EnumPlatform;
import io.github.trethore.jcefgithub.UnsupportedPlatformException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.cef.CefSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneCefInstaller {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefInstaller.class);
  private static final Gson GSON = new Gson();
  private static final String BUILD_METADATA = "/jcefgithub_build_meta.json";

  private GrapheneCefInstaller() {}

  public static CefAppBuilder createBuilder(GrapheneGlobalConfig config) {
    GrapheneGlobalConfig validatedConfig = Objects.requireNonNull(config, "config");
    Path installPath = resolveInstallPath(validatedConfig);
    File installDirectory = installPath.toFile();
    CefAppBuilder builder = new CefAppBuilder();
    builder.setInstallDir(installDirectory);
    configureRuntimePaths(builder, installDirectory);
    configureExtensions(builder, validatedConfig);
    configureRemoteDebugging(builder, validatedConfig);
    configurePlatformCompatibility(builder);
    builder.addJcefArgs("--force-color-profile=srgb");

    try {
      Path cacheDirectory = Files.createDirectories(installPath.resolve("cache"));
      builder.getCefSettings().cache_path = cacheDirectory.toAbsolutePath().toString();
      builder.getCefSettings().root_cache_path = cacheDirectory.toAbsolutePath().toString();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to create Graphene browser cache directory", exception);
    }

    builder.getCefSettings().log_file = installPath.resolve("logs.txt").toAbsolutePath().toString();
    builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
    builder.getCefSettings().windowless_rendering_enabled = true;
    return builder;
  }

  public static String currentPlatformIdentifier() {
    try {
      return EnumPlatform.getCurrentPlatform().getIdentifier();
    } catch (UnsupportedPlatformException exception) {
      throw new IllegalStateException("Unsupported JCEF platform", exception);
    }
  }

  static Path resolveInstallPath(GrapheneGlobalConfig config) {
    return config
        .resolvedBrowserRuntimePath()
        .toAbsolutePath()
        .normalize()
        .resolve(resolveJcefVersion())
        .resolve(currentPlatformIdentifier());
  }

  private static String resolveJcefVersion() {
    try (InputStream input = GrapheneCefInstaller.class.getResourceAsStream(BUILD_METADATA)) {
      if (input == null) {
        throw new IOException(BUILD_METADATA + " is missing");
      }
      JsonObject metadata =
          GSON.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonObject.class);
      if (metadata == null || !metadata.has("version")) {
        throw new IOException("Missing version in " + BUILD_METADATA);
      }
      String version = metadata.get("version").getAsString();
      if (version == null || version.isBlank()) {
        throw new IOException("Blank version in " + BUILD_METADATA);
      }
      return version;
    } catch (IOException | JsonParseException exception) {
      String implementationVersion = CefAppBuilder.class.getPackage().getImplementationVersion();
      if (implementationVersion == null || implementationVersion.isBlank()) {
        throw new IllegalStateException("Failed to resolve jcefgithub version", exception);
      }
      LOGGER.warn("Using jcefgithub implementation version {}", implementationVersion, exception);
      return implementationVersion;
    }
  }

  private static void configureRuntimePaths(CefAppBuilder builder, File installDirectory) {
    if (isMac()) {
      return;
    }
    String installPath = installDirectory.getAbsolutePath();
    builder.getCefSettings().resources_dir_path = installPath;
    builder.getCefSettings().locales_dir_path = installPath + File.separator + "locales";
    if (isLinux()) {
      builder.getCefSettings().browser_subprocess_path =
          installPath + File.separator + "jcef_helper";
    }
  }

  private static void configureExtensions(CefAppBuilder builder, GrapheneGlobalConfig config) {
    LinkedHashSet<Path> extensions = new LinkedHashSet<>();
    for (Path configuredFolder : config.extensionFolders()) {
      extensions.addAll(collectExtensions(configuredFolder.toAbsolutePath().normalize()));
    }
    if (extensions.isEmpty()) {
      builder.addJcefArgs("--disable-extensions");
      return;
    }
    String extensionArgument = String.join(",", extensions.stream().map(Path::toString).toList());
    builder.addJcefArgs("--disable-extensions-except=" + extensionArgument);
    builder.addJcefArgs("--load-extension=" + extensionArgument);
  }

  private static List<Path> collectExtensions(Path folder) {
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    if (Files.isRegularFile(folder.resolve("manifest.json"))) {
      return List.of(folder);
    }
    try (Stream<Path> children = Files.list(folder)) {
      return children
          .filter(Files::isDirectory)
          .filter(path -> Files.isRegularFile(path.resolve("manifest.json")))
          .sorted()
          .toList();
    } catch (IOException exception) {
      LOGGER.warn("Failed to inspect extension folder {}", folder, exception);
      return List.of();
    }
  }

  private static void configureRemoteDebugging(CefAppBuilder builder, GrapheneGlobalConfig config) {
    GrapheneRemoteDebugConfig remoteDebug =
        config.remoteDebugging().orElse(GrapheneRemoteDebugConfig.disabled());
    if (!remoteDebug.enabled()) {
      builder.getCefSettings().remote_debugging_port = 0;
      return;
    }
    builder.getCefSettings().remote_debugging_port =
        remoteDebug.fixedPort().orElseGet(GrapheneCefInstaller::findAvailablePort);
    remoteDebug
        .allowedOrigins()
        .ifPresent(origins -> builder.addJcefArgs("--remote-allow-origins=" + origins));
  }

  private static void configurePlatformCompatibility(CefAppBuilder builder) {
    if (!isLinux()) {
      return;
    }
    builder.addJcefArgs("--no-sandbox", "--password-store=basic");
  }

  private static int findAvailablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      return 9222;
    }
  }

  private static boolean isLinux() {
    return osName().contains("linux");
  }

  private static boolean isMac() {
    return osName().contains("mac");
  }

  private static String osName() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  }
}
