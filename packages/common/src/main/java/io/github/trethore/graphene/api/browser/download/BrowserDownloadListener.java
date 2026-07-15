package io.github.trethore.graphene.api.browser.download;

@FunctionalInterface
public interface BrowserDownloadListener {
  /**
   * Receives immutable download snapshots on the browser callback thread. Implementations must not
   * block. Listener failures are isolated from other listeners.
   */
  void onDownloadChanged(BrowserDownload download);
}
