package io.github.trethore.graphene.api.browser;

@FunctionalInterface
public interface BrowserConsoleMessageListener {
  void onConsoleMessage(BrowserConsoleMessage message);
}
