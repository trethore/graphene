package io.github.trethore.graphene.api.browser;

/** Receives browser loading-state and frame-load events. */
public interface BrowserLoadListener {
  default void onLoadingStateChanged(BrowserLoadingState state) {}

  default void onLoadStarted(BrowserLoadStarted event) {}

  default void onLoadCompleted(BrowserLoadCompleted event) {}

  default void onLoadFailed(BrowserLoadFailed event) {}
}
