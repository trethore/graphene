package io.github.trethore.graphene.api.browser;

public interface BrowserSessions {
  default BrowserSession create(String url) {
    return create(url, BrowserOptions.defaults(), 1, 1);
  }

  default BrowserSession create(String url, BrowserOptions options) {
    return create(url, options, 1, 1);
  }

  BrowserSession create(String url, BrowserOptions options, int width, int height);
}
