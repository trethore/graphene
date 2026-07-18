package io.github.trethore.graphene.api.browser;

/** Receives changes to a browser session's main-frame URL. */
@FunctionalInterface
public interface BrowserUrlListener {
  void onUrlChanged(String url);
}
