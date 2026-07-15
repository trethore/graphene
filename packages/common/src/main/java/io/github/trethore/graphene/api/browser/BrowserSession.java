package io.github.trethore.graphene.api.browser;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.browser.download.BrowserDownload;
import io.github.trethore.graphene.api.browser.download.BrowserDownloadListener;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserScrollInput;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import java.util.List;

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

  void resize(int width, int height);

  void setFocused(boolean focused);

  void sendPointerInput(BrowserPointerInput input);

  void sendScrollInput(BrowserScrollInput input);

  void sendKeyInput(BrowserKeyInput input);

  void sendTextInput(BrowserTextInput input);

  BrowserFrame latestFrame();

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

  @Override
  void close();
}
