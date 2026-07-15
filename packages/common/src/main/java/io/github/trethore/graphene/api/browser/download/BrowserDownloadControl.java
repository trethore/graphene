package io.github.trethore.graphene.api.browser.download;

@FunctionalInterface
public interface BrowserDownloadControl {
  /**
   * Requests cancellation from any thread and returns whether this was the first effective
   * cancellation request. Terminal downloads return {@code false}.
   */
  boolean cancel();
}
