package io.github.trethore.graphene.internal.url;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.url.AssetId;
import org.junit.jupiter.api.Test;

final class GrapheneAppUrlsTest {
  @Test
  void buildsAppUrls() {
    assertEquals(
        "app://assets/my-mod/web/index.html", GrapheneAppUrls.url("my-mod", "/web/index.html"));
    assertEquals(
        "app://assets/my-mod/web/index.html",
        GrapheneAppUrls.url(AssetId.of("my-mod", "web/index.html")));
    assertEquals(
        "app://assets/my-mod/index.html", GrapheneAppUrls.assets("my-mod").url("index.html"));
  }

  @Test
  void normalizesSupportedAppUrlForms() {
    assertEquals(
        "assets/my-mod/web/a b.html",
        GrapheneAppUrls.normalizeResourcePath("app://assets/my-mod/web/a%20b.html?x=1#section"));
    assertEquals(
        "assets/my-mod/web/a b.html",
        GrapheneAppUrls.normalizeResourcePath("app://my-mod/web/a%20b.html?x=1#section"));
    assertEquals(
        "assets/my-mod/web/a b.html",
        GrapheneAppUrls.normalizeResourcePath("app:///assets/my-mod/web/a%20b.html?x=1#section"));
  }

  @Test
  void rejectsUnsupportedAndUnsafeResourceUrls() {
    assertEquals("", GrapheneAppUrls.normalizeResourcePath("https://example.com/index.html"));
    assertEquals("", GrapheneAppUrls.normalizeResourcePath("app://assets/my-mod/../secret.txt"));
    assertEquals(
        "", GrapheneAppUrls.normalizeResourcePath("app://assets/my-mod/%2e%2e/secret.txt"));
  }
}
