package io.github.trethore.graphene.api.browser;

/** Receives browser console messages. */
@FunctionalInterface
public interface BrowserConsoleMessageListener {
  void onConsoleMessage(BrowserConsoleMessage message);
}
