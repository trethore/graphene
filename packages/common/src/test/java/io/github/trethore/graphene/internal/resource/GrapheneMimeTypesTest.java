package io.github.trethore.graphene.internal.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GrapheneMimeTypesTest {
  @Test
  void resolvesKnownMimeTypesCaseInsensitively() {
    assertEquals("text/html", GrapheneMimeTypes.resolve("index.HTML"));
    assertEquals("text/javascript", GrapheneMimeTypes.resolve("web/app.js"));
    assertEquals("application/wasm", GrapheneMimeTypes.resolve("web/module.wasm"));
  }

  @Test
  void fallsBackToBinaryForUnknownExtensions() {
    assertEquals(
        GrapheneMimeTypes.DEFAULT_MIME_TYPE, GrapheneMimeTypes.resolve("web/custom.unknown"));
    assertEquals(GrapheneMimeTypes.DEFAULT_MIME_TYPE, GrapheneMimeTypes.resolve("README"));
  }
}
