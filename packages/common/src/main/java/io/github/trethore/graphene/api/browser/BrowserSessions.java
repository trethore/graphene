package io.github.trethore.graphene.api.browser;

/** Creates browser sessions owned by a Graphene consumer context. */
public interface BrowserSessions {
  default BrowserSession create(String url) {
    return create(url, BrowserOptions.defaults(), 1, 1);
  }

  default BrowserSession create(String url, BrowserOptions options) {
    return create(url, options, 1, 1);
  }

  /**
   * Creates a browser session while the Graphene runtime is running.
   *
   * @throws BrowserRuntimeUnavailableException if the runtime is not running
   */
  BrowserSession create(String url, BrowserOptions options, int width, int height);
}
