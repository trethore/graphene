package io.github.trethore.graphene.api.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class AssetIdTest {
  @Test
  void createsValidatedAssetIdentifier() {
    AssetId assetId = AssetId.of("my-mod", "/web/index.html");

    assertEquals("my-mod", assetId.namespace());
    assertEquals("web/index.html", assetId.path());
  }

  @Test
  void rejectsInvalidNamespaces() {
    assertThrows(IllegalArgumentException.class, () -> AssetId.of("My Mod", "index.html"));
    assertThrows(IllegalArgumentException.class, () -> AssetId.of("", "index.html"));
  }

  @Test
  void rejectsUnsafePaths() {
    assertThrows(IllegalArgumentException.class, () -> AssetId.of("my-mod", ""));
    assertThrows(IllegalArgumentException.class, () -> AssetId.of("my-mod", "../index.html"));
    assertThrows(IllegalArgumentException.class, () -> AssetId.of("my-mod", "web//index.html"));
    assertThrows(IllegalArgumentException.class, () -> AssetId.of("my-mod", "web\\index.html"));
    assertThrows(IllegalArgumentException.class, () -> AssetId.of("my-mod", "web/index?.html"));
  }
}
