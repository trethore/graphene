package io.github.trethore.graphene.api.browser.bridge;

/** Identifies how Graphene classified a document for bridge exposure decisions. */
public enum BrowserBridgeDocumentSource {
  GRAPHENE_APP,
  GRAPHENE_CLASSPATH,
  GRAPHENE_HTTP,
  OTHER;

  public boolean grapheneOwned() {
    return this != OTHER;
  }
}
