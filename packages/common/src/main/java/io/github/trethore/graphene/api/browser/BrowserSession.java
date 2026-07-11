package io.github.trethore.graphene.api.browser;

import io.github.trethore.graphene.api.bridge.GrapheneBridge;

public interface BrowserSession extends AutoCloseable {
  BrowserOptions options();

  String currentUrl();

  boolean isClosed();

  void navigate(String url);

  void goBack();

  void goForward();

  void reload();

  void stopLoading();

  void executeScript(String script);

  void resize(int width, int height);

  BrowserFrame latestFrame();

  GrapheneBridge bridge();

  void addLoadListener(BrowserLoadListener listener);

  void removeLoadListener(BrowserLoadListener listener);

  @Override
  void close();
}
