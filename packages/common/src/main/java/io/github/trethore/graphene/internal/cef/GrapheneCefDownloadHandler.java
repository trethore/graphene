package io.github.trethore.graphene.internal.cef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneCefDownloadHandler extends CefDownloadHandlerAdapter {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefDownloadHandler.class);
  private static final String FALLBACK_NAME = "download";

  private final Path downloadDirectory;
  private final ConcurrentHashMap<Integer, Path> activeDownloads = new ConcurrentHashMap<>();

  GrapheneCefDownloadHandler() {
    this(
        Path.of(System.getProperty("user.home", "."), "Downloads", "grapheneui")
            .toAbsolutePath()
            .normalize());
  }

  GrapheneCefDownloadHandler(Path downloadDirectory) {
    this.downloadDirectory = downloadDirectory.toAbsolutePath().normalize();
  }

  @Override
  public boolean onBeforeDownload(
      CefBrowser browser,
      CefDownloadItem downloadItem,
      String suggestedName,
      CefBeforeDownloadCallback callback) {
    if (callback == null) {
      return false;
    }
    Path directory = createDownloadDirectory();
    Path target = uniquePath(directory, suggestedName, new HashSet<>(activeDownloads.values()));
    if (downloadItem != null) {
      activeDownloads.put(downloadItem.getId(), target);
    }
    callback.Continue(target.toString(), false);
    return true;
  }

  @Override
  public void onDownloadUpdated(
      CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
    if (downloadItem != null && !downloadItem.isInProgress()) {
      activeDownloads.remove(downloadItem.getId());
    }
  }

  static Path uniquePath(Path directory, String suggestedName, Set<Path> reservedPaths) {
    String fileName = sanitizeFileName(suggestedName);
    int extensionIndex = fileName.lastIndexOf('.');
    String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    String extension = extensionIndex > 0 ? fileName.substring(extensionIndex) : "";
    Path candidate = directory.resolve(fileName).toAbsolutePath().normalize();
    int suffix = 1;
    while (Files.exists(candidate) || reservedPaths.contains(candidate)) {
      candidate =
          directory
              .resolve(baseName + " (" + suffix++ + ")" + extension)
              .toAbsolutePath()
              .normalize();
    }
    return candidate;
  }

  private Path createDownloadDirectory() {
    try {
      return Files.createDirectories(downloadDirectory);
    } catch (IOException exception) {
      Path fallback = Path.of(System.getProperty("java.io.tmpdir", ".")).toAbsolutePath();
      LOGGER.warn("Failed to create download directory {}, using {}", downloadDirectory, fallback);
      return fallback;
    }
  }

  private static String sanitizeFileName(String suggestedName) {
    String fileName =
        suggestedName == null || suggestedName.isBlank() ? FALLBACK_NAME : suggestedName;
    String sanitized =
        stripTrailingDotsAndSpaces(fileName.replaceAll("[\\x00-\\x1F<>:\"/\\\\|?*]", "_").trim());
    if (sanitized.isBlank()) {
      return FALLBACK_NAME;
    }
    int dotIndex = sanitized.indexOf('.');
    String baseName = dotIndex > 0 ? sanitized.substring(0, dotIndex) : sanitized;
    String upperName = baseName.toUpperCase(Locale.ROOT);
    if (isReservedWindowsName(upperName)) {
      return "_" + sanitized;
    }
    return sanitized;
  }

  private static String stripTrailingDotsAndSpaces(String fileName) {
    int endIndex = fileName.length();
    while (endIndex > 0) {
      char character = fileName.charAt(endIndex - 1);
      if (character != '.' && character != ' ') {
        break;
      }
      endIndex--;
    }
    return fileName.substring(0, endIndex);
  }

  private static boolean isReservedWindowsName(String fileName) {
    if (fileName.equals("CON")
        || fileName.equals("PRN")
        || fileName.equals("AUX")
        || fileName.equals("NUL")) {
      return true;
    }
    if (fileName.length() != 4) {
      return false;
    }
    String prefix = fileName.substring(0, 3);
    char suffix = fileName.charAt(3);
    return (prefix.equals("COM") || prefix.equals("LPT")) && suffix >= '1' && suffix <= '9';
  }
}
