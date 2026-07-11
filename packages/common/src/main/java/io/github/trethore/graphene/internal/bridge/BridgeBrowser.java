package io.github.trethore.graphene.internal.bridge;

public interface BridgeBrowser {
  void executeScript(String script, String sourceUrl);

  String currentUrl();

  boolean hasDocument();

  int identifier();
}
