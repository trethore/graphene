package io.github.trethore.graphene.api.browser;

public interface BrowserLoadListener {
  default void onLoadingStateChanged(BrowserLoadingState state) {
    // Optional listener callback.
  }

  default void onLoadStarted(BrowserLoadStarted event) {
    // Optional listener callback.
  }

  default void onLoadCompleted(BrowserLoadCompleted event) {
    // Optional listener callback.
  }

  default void onLoadFailed(BrowserLoadFailed event) {
    // Optional listener callback.
  }
}
