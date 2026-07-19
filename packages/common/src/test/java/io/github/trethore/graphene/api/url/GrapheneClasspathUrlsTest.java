package io.github.trethore.graphene.api.url;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GrapheneClasspathUrlsTest {
  @Test
  void buildsClasspathUrls() {
    assertEquals(
        "classpath:///assets/grapheneui/web/index.html",
        GrapheneClasspathUrls.assets().url("web/index.html"));
    assertEquals(
        "classpath:///assets/my-mod/web/index.html",
        GrapheneClasspathUrls.url("my-mod", "/web/index.html"));
    assertEquals(
        "classpath:///assets/my-mod/web/index.html",
        GrapheneClasspathUrls.url(AssetId.of("my-mod", "web/index.html")));
  }

  @Test
  void namespaceScopedAssetsUseTheirNamespace() {
    GrapheneAssetUrls urls = GrapheneClasspathUrls.assets("other-mod");

    assertEquals("classpath:///assets/other-mod/index.html", urls.url("index.html"));
  }

  @Test
  void normalizesClasspathResourceUrls() {
    assertEquals(
        "assets/my-mod/web/a b.html",
        GrapheneClasspathUrls.normalizeResourcePath(
            "classpath:///assets/my-mod/web/a%20b.html?x=1#section"));
    assertEquals("", GrapheneClasspathUrls.normalizeResourcePath("https://example.com/index.html"));
    assertEquals("", GrapheneClasspathUrls.normalizeResourcePath("classpath:///invalid/path"));
    assertEquals(
        "",
        GrapheneClasspathUrls.normalizeResourcePath(
            "classpath:///assets/my-mod/%2e%2e/secret.txt"));
  }
}
