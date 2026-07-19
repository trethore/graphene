package io.github.trethore.graphene.internal.url;

import io.github.trethore.graphene.api.url.AssetId;
import io.github.trethore.graphene.api.url.GrapheneAssetUrls;
import java.util.Objects;
import java.util.function.Supplier;

public final class GrapheneHttpUrls {
  private static final String MODS_ROOT = "mods";

  private final Supplier<String> baseUrlSupplier;

  public GrapheneHttpUrls(Supplier<String> baseUrlSupplier) {
    this.baseUrlSupplier = Objects.requireNonNull(baseUrlSupplier, "baseUrlSupplier");
  }

  public GrapheneAssetUrls assets(String namespace) {
    return new GrapheneHttpAssetUrls(namespace);
  }

  public String modUrl(String modId, String path) {
    String normalizedModId = AssetId.normalizeNamespace(modId);
    String normalizedPath = AssetId.normalizePath(path);
    return baseUrl() + "/" + MODS_ROOT + "/" + normalizedModId + "/" + normalizedPath;
  }

  private String baseUrl() {
    String baseUrl = Objects.requireNonNull(baseUrlSupplier.get(), "baseUrl").trim();
    if (baseUrl.isBlank()) {
      throw new IllegalStateException("Graphene HTTP server is not running");
    }

    while (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }

  private final class GrapheneHttpAssetUrls extends AbstractGrapheneAssetUrls {
    private GrapheneHttpAssetUrls(String defaultNamespace) {
      super(defaultNamespace);
    }

    @Override
    protected String rootPrefix() {
      return baseUrl() + "/" + ASSET_HOST + "/";
    }
  }
}
