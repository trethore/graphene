package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.download.BrowserDownload;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadControl;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadId;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadListener;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadPolicy;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadState;
import io.github.trethore.graphene.internal.event.GrapheneListenerList;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneCefDownloadRegistry implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCefDownloadRegistry.class);
  private static final String FALLBACK_FILE_NAME = "download";
  private static final AtomicLong NEXT_DOWNLOAD_ID = new AtomicLong();
  private static final Pattern INVALID_FILE_NAME_CHARACTERS =
      Pattern.compile("[\\x00-\\x1F<>:\"/\\\\|?*]");

  private final BrowserSession session;
  private final BrowserDownloadPolicy policy;
  private final Map<Integer, ActiveDownload> activeDownloads = new LinkedHashMap<>();
  private final GrapheneListenerList<BrowserDownloadListener> listeners =
      new GrapheneListenerList<>();
  private boolean closed;

  GrapheneCefDownloadRegistry(BrowserSession session, BrowserDownloadPolicy policy) {
    this.session = Objects.requireNonNull(session, "session");
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  boolean onBeforeDownload(
      CefDownloadItem item, String suggestedName, CefBeforeDownloadCallback callback) {
    if (callback == null || item == null || !item.isValid()) {
      return false;
    }
    int cefIdentifier = item.getId();
    ActiveDownload download = createDownload(item, suggestedName);
    ActiveDownload replaced;
    synchronized (this) {
      if (closed) {
        return false;
      }
      replaced = activeDownloads.put(cefIdentifier, download);
    }
    if (replaced != null) {
      replaced.abort();
    }
    publish(download.snapshot());

    BrowserDownloadPolicy.Decision decision = decide(download.request(session));
    if (decision instanceof BrowserDownloadPolicy.Cancel || download.cancellationRequested()) {
      finish(cefIdentifier, download, BrowserDownloadState.CANCELED);
      return false;
    }

    try {
      if (decision instanceof BrowserDownloadPolicy.SaveTo(Path targetPath)) {
        download.targetPath(targetPath);
        callback.Continue(targetPath.toString(), false);
      } else if (decision instanceof BrowserDownloadPolicy.ShowSaveDialog) {
        callback.Continue("", true);
      } else {
        finish(cefIdentifier, download, BrowserDownloadState.CANCELED);
        return false;
      }
      return true;
    } catch (RuntimeException exception) {
      LOGGER.error("Failed to start browser download {}", download.id(), exception);
      finish(cefIdentifier, download, BrowserDownloadState.FAILED);
      return false;
    }
  }

  void onDownloadUpdated(CefDownloadItem item, CefDownloadItemCallback callback) {
    if (item == null || !item.isValid()) {
      return;
    }
    int cefIdentifier = item.getId();
    ActiveDownload download;
    synchronized (this) {
      download = activeDownloads.get(cefIdentifier);
    }
    if (download == null) {
      return;
    }

    try {
      if (!download.update(item, callback)) {
        return;
      }
    } catch (RuntimeException exception) {
      LOGGER.error("Failed to update browser download {}", download.id(), exception);
      finish(cefIdentifier, download, BrowserDownloadState.FAILED);
      return;
    }
    BrowserDownload snapshot = download.snapshot();
    publish(snapshot);
    if (download.terminal()) {
      synchronized (this) {
        activeDownloads.remove(cefIdentifier, download);
      }
      download.releaseCallback();
    }
  }

  synchronized List<BrowserDownload> activeDownloads() {
    return activeDownloads.values().stream().map(ActiveDownload::snapshot).toList();
  }

  GrapheneSubscription subscribe(BrowserDownloadListener listener) {
    return listeners.subscribe(listener);
  }

  @Override
  public void close() {
    List<ActiveDownload> downloads;
    synchronized (this) {
      if (closed) {
        return;
      }
      closed = true;
      downloads = new ArrayList<>(activeDownloads.values());
      activeDownloads.clear();
    }
    downloads.forEach(ActiveDownload::abort);
    listeners.close();
  }

  private ActiveDownload createDownload(CefDownloadItem item, String suggestedName) {
    String resolvedSuggestedName = suggestedName;
    if (resolvedSuggestedName == null) {
      resolvedSuggestedName = Objects.requireNonNullElse(item.getSuggestedFileName(), "");
    }
    return new ActiveDownload(
        new BrowserDownloadId(NEXT_DOWNLOAD_ID.getAndIncrement()),
        item,
        sanitizeFileName(resolvedSuggestedName));
  }

  private BrowserDownloadPolicy.Decision decide(BrowserDownloadPolicy.Request request) {
    try {
      BrowserDownloadPolicy.Decision decision = policy.decide(request);
      return decision == null ? BrowserDownloadPolicy.Decision.cancel() : decision;
    } catch (RuntimeException exception) {
      LOGGER.error("Unhandled Graphene browser download policy exception", exception);
      return BrowserDownloadPolicy.Decision.cancel();
    }
  }

  private void finish(
      int cefIdentifier, ActiveDownload download, BrowserDownloadState terminalState) {
    synchronized (this) {
      if (!activeDownloads.remove(cefIdentifier, download)) {
        return;
      }
    }
    download.state(terminalState);
    publish(download.snapshot());
    download.releaseCallback();
  }

  private void publish(BrowserDownload download) {
    listeners.dispatch(
        listener -> listener.onDownloadChanged(download), LOGGER, "browser download listener");
  }

  private static String sanitizeFileName(String suggestedName) {
    String fileName = suggestedName.isBlank() ? FALLBACK_FILE_NAME : suggestedName;
    String sanitized =
        stripTrailingDotsAndSpaces(
            INVALID_FILE_NAME_CHARACTERS.matcher(fileName).replaceAll("_").trim());
    if (sanitized.isBlank()) {
      return FALLBACK_FILE_NAME;
    }
    int dotIndex = sanitized.indexOf('.');
    String baseName = dotIndex > 0 ? sanitized.substring(0, dotIndex) : sanitized;
    if (isReservedWindowsName(baseName.toUpperCase(Locale.ROOT))) {
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

  private static final class ActiveDownload implements BrowserDownloadControl {
    private final GrapheneCefDownloadSnapshot.Metadata metadata;
    private Path targetPath;
    private long receivedBytes;
    private long totalBytes;
    private long currentSpeed;
    private int percentComplete;
    private Instant startedAt;
    private Instant endedAt;
    private BrowserDownloadState state = BrowserDownloadState.REQUESTED;
    private CefDownloadItemCallback callback;
    private boolean cancellationRequested;

    private ActiveDownload(BrowserDownloadId id, CefDownloadItem item, String suggestedFileName) {
      this.metadata =
          new GrapheneCefDownloadSnapshot.Metadata(
              id,
              Objects.requireNonNullElse(item.getURL(), ""),
              Objects.requireNonNullElse(suggestedFileName, ""),
              Objects.requireNonNullElse(item.getMimeType(), ""),
              Objects.requireNonNullElse(item.getContentDisposition(), ""));
      updateProgress(item);
    }

    private BrowserDownloadPolicy.Request request(BrowserSession session) {
      return new BrowserDownloadPolicy.Request(
          session,
          metadata.id(),
          metadata.url(),
          metadata.suggestedFileName(),
          metadata.mimeType(),
          metadata.contentDisposition(),
          optionalLong(totalBytes));
    }

    private synchronized BrowserDownload snapshot() {
      return new GrapheneCefDownloadSnapshot(
          metadata,
          state,
          targetPath,
          new GrapheneCefDownloadSnapshot.Progress(
              receivedBytes, totalBytes, currentSpeed, percentComplete, startedAt, endedAt),
          this);
    }

    private boolean update(CefDownloadItem item, CefDownloadItemCallback callback) {
      boolean cancelUpdatedDownload;
      boolean updated;
      synchronized (this) {
        if (terminalLocked()) {
          cancelUpdatedDownload = callback != null;
          updated = false;
        } else {
          this.callback = callback;
          String fullPath = item.getFullPath();
          if (fullPath != null && !fullPath.isBlank()) {
            targetPath = Path.of(fullPath).toAbsolutePath().normalize();
          }
          updateProgress(item);
          if (item.isComplete()) {
            state = BrowserDownloadState.COMPLETED;
          } else if (item.isCanceled()) {
            state = BrowserDownloadState.CANCELED;
          } else if (item.isInProgress()) {
            state = BrowserDownloadState.IN_PROGRESS;
          } else {
            state = BrowserDownloadState.FAILED;
          }
          cancelUpdatedDownload = cancellationRequested && callback != null && !terminalLocked();
          updated = true;
        }
      }
      if (cancelUpdatedDownload) {
        cancelCallback(callback);
      }
      return updated;
    }

    private synchronized void targetPath(Path targetPath) {
      this.targetPath = targetPath;
    }

    private synchronized void state(BrowserDownloadState state) {
      this.state = state;
    }

    private synchronized boolean cancellationRequested() {
      return cancellationRequested;
    }

    private BrowserDownloadId id() {
      return metadata.id();
    }

    @Override
    public boolean cancel() {
      CefDownloadItemCallback currentCallback;
      synchronized (this) {
        if (cancellationRequested || terminalLocked()) {
          return false;
        }
        cancellationRequested = true;
        currentCallback = callback;
      }
      if (currentCallback != null) {
        cancelCallback(currentCallback);
      }
      return true;
    }

    private synchronized void releaseCallback() {
      callback = null;
    }

    private void abort() {
      CefDownloadItemCallback currentCallback;
      synchronized (this) {
        if (terminalLocked()) {
          callback = null;
          return;
        }
        cancellationRequested = true;
        state = BrowserDownloadState.CANCELED;
        currentCallback = callback;
        callback = null;
      }
      if (currentCallback != null) {
        cancelCallback(currentCallback);
      }
    }

    private void cancelCallback(CefDownloadItemCallback callback) {
      try {
        callback.cancel();
      } catch (RuntimeException exception) {
        LOGGER.error("Failed to cancel browser download {}", metadata.id(), exception);
      }
    }

    private void updateProgress(CefDownloadItem item) {
      receivedBytes = nonNegative(item.getReceivedBytes());
      totalBytes = item.getTotalBytes();
      currentSpeed = nonNegative(item.getCurrentSpeed());
      percentComplete = item.getPercentComplete();
      startedAt = startTime(item);
      endedAt = endTime(item);
    }

    private synchronized boolean terminal() {
      return terminalLocked();
    }

    private boolean terminalLocked() {
      return state == BrowserDownloadState.COMPLETED
          || state == BrowserDownloadState.CANCELED
          || state == BrowserDownloadState.FAILED;
    }

    private static long nonNegative(long value) {
      return Math.max(0, value);
    }

    private static OptionalLong optionalLong(long value) {
      return value < 0 ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @SuppressWarnings("java:S2143")
    private static Instant startTime(CefDownloadItem item) {
      Date value = item.getStartTime();
      return value == null ? null : value.toInstant();
    }

    @SuppressWarnings("java:S2143")
    private static Instant endTime(CefDownloadItem item) {
      Date value = item.getEndTime();
      return value == null ? null : value.toInstant();
    }
  }
}
