package io.github.trethore.graphene.internal.bridge;

import io.github.trethore.graphene.api.browser.bridge.BrowserBridgeDocumentSource;
import io.github.trethore.graphene.api.browser.bridge.BrowserBridgeOrigin;
import io.github.trethore.graphene.api.url.GrapheneClasspathUrls;
import io.github.trethore.graphene.internal.url.GrapheneAppUrls;
import java.util.Objects;

final class GrapheneBridgeDocumentClassifier {
  private final BrowserBridgeOrigin grapheneHttpOrigin;

  GrapheneBridgeDocumentClassifier(String grapheneHttpBaseUrl) {
    this.grapheneHttpOrigin =
        BrowserBridgeOrigin.fromUrl(
                Objects.requireNonNull(grapheneHttpBaseUrl, "grapheneHttpBaseUrl"))
            .orElse(null);
  }

  BrowserBridgeDocumentSource classify(String documentUrl, BrowserBridgeOrigin documentOrigin) {
    String validatedDocumentUrl = Objects.requireNonNull(documentUrl, "documentUrl");
    if (!GrapheneAppUrls.normalizeResourcePath(validatedDocumentUrl).isBlank()) {
      return BrowserBridgeDocumentSource.GRAPHENE_APP;
    }
    if (!GrapheneClasspathUrls.normalizeResourcePath(validatedDocumentUrl).isBlank()) {
      return BrowserBridgeDocumentSource.GRAPHENE_CLASSPATH;
    }
    if (grapheneHttpOrigin != null && grapheneHttpOrigin.equals(documentOrigin)) {
      return BrowserBridgeDocumentSource.GRAPHENE_HTTP;
    }
    return BrowserBridgeDocumentSource.OTHER;
  }
}
