package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.GrapheneCore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class GrapheneCefDownloadHandler extends CefDownloadHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefDownloadHandler.class);
    private static final String FALLBACK_FILENAME = "download";
    private static final Path PRIMARY_DOWNLOAD_DIRECTORY = Path.of(
            System.getProperty("user.home", "."),
            "Downloads",
            GrapheneCore.ID
    );

    private final Map<Integer, Path> activeDownloads = new ConcurrentHashMap<>();

    private static String downloadUrl(CefDownloadItem downloadItem) {
        if (downloadItem == null || downloadItem.getURL() == null || downloadItem.getURL().isBlank()) {
            return "unknown";
        }

        return downloadItem.getURL();
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    private static String sanitizeFileName(String suggestedName) {
        String sanitized = firstNonBlank(suggestedName, FALLBACK_FILENAME)
                .replaceAll("[\\x00-\\x1F<>:\"/\\\\|?*]", "_")
                .replaceAll("[. ]+$", "")
                .trim();

        if (sanitized.isBlank()) {
            return FALLBACK_FILENAME;
        }

        int dotIndex = sanitized.indexOf('.');
        String baseName = dotIndex > 0 ? sanitized.substring(0, dotIndex) : sanitized;
        String normalizedBaseName = baseName.toUpperCase(Locale.ROOT);
        if (
                normalizedBaseName.equals("CON")
                        || normalizedBaseName.equals("PRN")
                        || normalizedBaseName.equals("AUX")
                        || normalizedBaseName.equals("NUL")
                        || normalizedBaseName.matches("COM[1-9]")
                        || normalizedBaseName.matches("LPT[1-9]")
        ) {
            return "_" + sanitized;
        }

        return sanitized;
    }

    private static Path ensureDownloadDirectory() {
        try {
            return Files.createDirectories(PRIMARY_DOWNLOAD_DIRECTORY.toAbsolutePath().normalize());
        } catch (Exception exception) {
            LOGGER.warn("Failed to prepare default download directory {}", PRIMARY_DOWNLOAD_DIRECTORY, exception);
            Path fallbackDirectory = Path.of(System.getProperty("java.io.tmpdir", ".")).toAbsolutePath().normalize();
            try {
                return Files.createDirectories(fallbackDirectory);
            } catch (Exception fallbackException) {
                LOGGER.warn("Failed to prepare fallback download directory {}", fallbackDirectory, fallbackException);
                return fallbackDirectory;
            }
        }
    }

    private static Path uniqueDownloadPath(Path downloadDirectory, String fileName) {
        Objects.requireNonNull(downloadDirectory, "downloadDirectory");
        String normalizedName = sanitizeFileName(fileName);

        int extensionIndex = normalizedName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? normalizedName.substring(0, extensionIndex) : normalizedName;
        String extension = extensionIndex > 0 ? normalizedName.substring(extensionIndex) : "";

        Path candidate = downloadDirectory.resolve(normalizedName);
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = downloadDirectory.resolve(baseName + " (" + suffix + ")" + extension);
            suffix++;
        }

        return candidate;
    }

    @Override
    public boolean onBeforeDownload(
            CefBrowser ignoredBrowser,
            CefDownloadItem downloadItem,
            String suggestedName,
            CefBeforeDownloadCallback callback
    ) {
        if (callback == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Suppressed download without callback (url={})", downloadUrl(downloadItem));
            }
            return false;
        }

        Path downloadDirectory = ensureDownloadDirectory();
        String fileName = firstNonBlank(suggestedName, downloadItem == null ? null : downloadItem.getSuggestedFileName());
        Path targetPath = uniqueDownloadPath(downloadDirectory, fileName).toAbsolutePath().normalize();

        if (downloadItem != null) {
            activeDownloads.put(downloadItem.getId(), targetPath);
        }
        callback.Continue(targetPath.toString(), false);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting download {} -> {}", downloadUrl(downloadItem), targetPath);
        }
        return true;
    }

    @Override
    public void onDownloadUpdated(
            CefBrowser ignoredBrowser,
            CefDownloadItem downloadItem,
            CefDownloadItemCallback ignoredCallback
    ) {
        if (downloadItem == null || downloadItem.isInProgress()) {
            return;
        }

        int downloadId = downloadItem.getId();
        Path expectedPath = activeDownloads.remove(downloadId);
        String resolvedPath = expectedPath == null ? downloadItem.getFullPath() : expectedPath.toString();
        if (downloadItem.isComplete()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Completed download {} -> {}", downloadUrl(downloadItem), resolvedPath);
            }
            return;
        }

        if (downloadItem.isCanceled()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Canceled download {}", downloadUrl(downloadItem));
            }
            return;
        }

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Interrupted download {}", downloadUrl(downloadItem));
        }
    }
}
