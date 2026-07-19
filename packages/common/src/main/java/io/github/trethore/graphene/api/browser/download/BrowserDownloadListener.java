package io.github.trethore.graphene.api.browser.download;

/** Receives immutable state changes for browser downloads. */
@FunctionalInterface
public interface BrowserDownloadListener {
  /**
   * Receives immutable download snapshots on the browser callback thread. Implementations must not
   * block. Listener failures are isolated from other listeners.
   */
  void onDownloadChanged(BrowserDownload download);
}
