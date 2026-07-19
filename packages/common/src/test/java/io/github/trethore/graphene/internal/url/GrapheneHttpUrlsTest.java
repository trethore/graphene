package io.github.trethore.graphene.internal.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.trethore.graphene.api.url.GrapheneAssetUrls;
import org.junit.jupiter.api.Test;

final class GrapheneHttpUrlsTest {
  @Test
  void buildsAssetAndMountedUrls() {
    GrapheneHttpUrls urls = new GrapheneHttpUrls(() -> "http://127.0.0.1:20000/");

    assertEquals(
        "http://127.0.0.1:20000/assets/my-mod/web/index.html",
        urls.assets("my-mod").url("web/index.html"));
    assertEquals(
        "http://127.0.0.1:20000/mods/my-mod/web/index.html",
        urls.modUrl("my-mod", "/web/index.html"));
  }

  @Test
  void rejectsUnavailableServerAndUnsafePaths() {
    GrapheneHttpUrls unavailableUrls = new GrapheneHttpUrls(() -> " ");
    GrapheneHttpUrls availableUrls = new GrapheneHttpUrls(() -> "http://127.0.0.1:20000");
    GrapheneAssetUrls unavailableAssets = unavailableUrls.assets("my-mod");

    assertThrows(IllegalStateException.class, () -> unavailableAssets.url("index.html"));
    assertThrows(
        IllegalArgumentException.class, () -> availableUrls.modUrl("my-mod", "../secret.txt"));
  }
}
