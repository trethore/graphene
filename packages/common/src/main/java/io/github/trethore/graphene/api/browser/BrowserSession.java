package io.github.trethore.graphene.api.browser;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.browser.download.BrowserDownload;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadListener;
import io.github.trethore.graphene.api.browser.find.BrowserFindDirection;
import io.github.trethore.graphene.api.browser.find.BrowserFindQuery;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserScrollInput;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import java.util.List;
import java.util.Optional;

/**
 * A closeable, off-screen browser session independent of any platform render surface. Closing the
 * session releases its browser resources and makes further state-changing operations invalid.
 */
@SuppressWarnings("unused")
public interface BrowserSession extends AutoCloseable {
  BrowserOptions options();

  /** Returns the current non-null main-frame URL. Blank URLs are exposed as {@code about:blank}. */
  String currentUrl();

  /** Returns the current non-null page title, or an empty string when the page has no title. */
  String currentTitle();

  boolean isClosed();

  boolean isLoading();

  boolean canGoBack();

  boolean canGoForward();

  BrowserCursor requestedCursor();

  void navigate(String url);

  void goBack();

  void goForward();

  void reload();

  void stopLoading();

  void executeScript(String script);

  /** Returns the current zoom level. The default level is {@code 0.0}. */
  double zoomLevel();

  /** Sets the zoom level. Positive values magnify and negative values reduce the page. */
  void setZoomLevel(double zoomLevel);

  /** Resets the zoom level to {@code 0.0}. */
  void resetZoom();

  /** Starts a new forward page-text search, replacing any active search. */
  void startFinding(BrowserFindQuery query);

  /** Continues the active page-text search in the requested direction. */
  void findNext(BrowserFindDirection direction);

  /** Stops the active page-text search and clears its selection. */
  void stopFinding();

  void resize(int width, int height);

  void setFocused(boolean focused);

  void sendPointerInput(BrowserPointerInput input);

  void sendScrollInput(BrowserScrollInput input);

  void sendKeyInput(BrowserKeyInput input);

  void sendTextInput(BrowserTextInput input);

  /**
   * Returns the latest complete frame snapshot, or an empty value before the first paint and after
   * session closure.
   */
  Optional<BrowserFrame> latestFrame();

  /**
   * Subscribes to frame snapshots delivered on the platform thread. Notifications are latest-only:
   * at most one is queued per session and intermediate frames may be coalesced. The returned
   * subscription is idempotently closeable.
   */
  GrapheneSubscription onFrame(BrowserFrameListener listener);

  GrapheneBridge bridge();

  /** Returns immutable snapshots of downloads that have not reached a terminal state. */
  List<BrowserDownload> activeDownloads();

  /**
   * Subscribes to download snapshots delivered on the browser callback thread. The returned
   * subscription is idempotently closeable.
   */
  GrapheneSubscription onDownloadChanged(BrowserDownloadListener listener);

  /** Subscribes to load events delivered on the platform thread. */
  GrapheneSubscription onLoad(BrowserLoadListener listener);

  /** Subscribes to deduplicated title changes delivered on the platform thread. */
  GrapheneSubscription onTitleChanged(BrowserTitleListener listener);

  /** Subscribes to deduplicated main-frame URL changes delivered on the platform thread. */
  GrapheneSubscription onUrlChanged(BrowserUrlListener listener);

  /** Subscribes to browser console messages delivered on the platform thread. */
  GrapheneSubscription onConsoleMessage(BrowserConsoleMessageListener listener);

  /** Closes the browser session. Repeated calls have no additional effect. */
  @Override
  void close();
}
