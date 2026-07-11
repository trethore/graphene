package io.github.trethore.graphene.api.browser;

import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserScrollInput;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;

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

  void setFocused(boolean focused);

  void sendPointerInput(BrowserPointerInput input);

  void sendScrollInput(BrowserScrollInput input);

  void sendKeyInput(BrowserKeyInput input);

  void sendTextInput(BrowserTextInput input);

  BrowserFrame latestFrame();

  GrapheneBridge bridge();

  void addLoadListener(BrowserLoadListener listener);

  void removeLoadListener(BrowserLoadListener listener);

  @Override
  void close();
}
