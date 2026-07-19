package io.github.trethore.graphene.api.browser;

/** Receives changes to a browser session's page title. */
@FunctionalInterface
public interface BrowserTitleListener {
  void onTitleChanged(String title);
}
