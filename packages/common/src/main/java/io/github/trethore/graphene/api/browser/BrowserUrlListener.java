package io.github.trethore.graphene.api.browser;

@FunctionalInterface
public interface BrowserUrlListener {
  void onUrlChanged(String url);
}
