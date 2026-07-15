package io.github.trethore.graphene.api.browser;

@FunctionalInterface
public interface BrowserTitleListener {
  void onTitleChanged(String title);
}
