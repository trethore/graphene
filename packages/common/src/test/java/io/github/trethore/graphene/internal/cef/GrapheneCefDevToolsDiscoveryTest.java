package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import io.github.trethore.graphene.api.devtools.DevToolsDiscoveryException;
import io.github.trethore.graphene.api.devtools.DevToolsPageTarget;
import io.github.trethore.graphene.api.devtools.DevToolsTargetAmbiguousException;
import io.github.trethore.graphene.api.devtools.DevToolsTargetNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrapheneCefDevToolsDiscoveryTest {
  private static final URI DISCOVERY_URI = URI.create("http://127.0.0.1:9333/json/list");

  @Test
  void queriesTheRemoteDiscoveryEndpoint() throws IOException {
    byte[] response =
        """
        [{
          "id":"page-1",
          "type":"page",
          "title":"Graphene",
          "url":"https://example.test/",
          "devtoolsFrontendUrl":"/devtools/inspector.html?ws=page-1"
        }]
        """
            .getBytes(StandardCharsets.UTF_8);
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/json/list",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();

    try {
      List<DevToolsPageTarget> targets =
          GrapheneCefDevToolsDiscovery.pageTargets(server.getAddress().getPort())
              .toCompletableFuture()
              .join();

      assertEquals(1, targets.size());
      assertEquals("page-1", targets.getFirst().id());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void parsesPageTargetsAndResolvesInspectorUris() {
    String response =
        """
        [
          {
            "id": "page-1",
            "type": "page",
            "title": "",
            "url": "about:blank",
            "devtoolsFrontendUrl": "/devtools/inspector.html?ws=page-1"
          },
          {
            "id": "worker-1",
            "type": "service_worker",
            "title": "Worker",
            "url": "https://example.test/worker.js",
            "devtoolsFrontendUrl": "/devtools/inspector.html?ws=worker-1"
          },
          {
            "id": "page-2",
            "type": "page",
            "title": "Graphene",
            "url": "https://example.test/",
            "devtoolsFrontendUrl": "devtools://bundled/inspector.html",
            "devtoolsFrontendUrlCompat": "https://frontend.test/inspector.html?ws=page-2"
          }
        ]
        """;

    List<DevToolsPageTarget> targets =
        GrapheneCefDevToolsDiscovery.parsePageTargets(response, DISCOVERY_URI);

    assertEquals(2, targets.size());
    assertEquals("", targets.getFirst().title());
    assertEquals(
        URI.create("http://127.0.0.1:9333/devtools/inspector.html?ws=page-1"),
        targets.getFirst().inspectorUri());
    assertEquals(
        URI.create("https://frontend.test/inspector.html?ws=page-2"),
        targets.getLast().inspectorUri());
  }

  @Test
  void resolvesUniqueUrlAndUsesTitleAsTieBreaker() {
    DevToolsPageTarget first = target("first", "First");
    DevToolsPageTarget second = target("second", "Second");

    assertEquals(
        second,
        GrapheneCefDevToolsDiscovery.resolveTarget(
            List.of(first, second), null, "https://example.test/", "Second"));
    assertEquals(
        first,
        GrapheneCefDevToolsDiscovery.resolveTarget(
            List.of(first, second), "first", "https://changed.test/", "Changed"));
  }

  @Test
  void reportsMissingAndAmbiguousSessionMatches() {
    DevToolsPageTarget first = target("first", "Same");
    DevToolsPageTarget second = target("second", "Same");
    List<DevToolsPageTarget> singleTarget = List.of(first);
    List<DevToolsPageTarget> duplicateMatches = List.of(first, second);

    assertThrows(
        DevToolsTargetNotFoundException.class,
        () ->
            GrapheneCefDevToolsDiscovery.resolveTarget(
                singleTarget, null, "https://missing.test/", "Missing"));
    DevToolsTargetAmbiguousException ambiguousException =
        assertThrows(
            DevToolsTargetAmbiguousException.class,
            () ->
                GrapheneCefDevToolsDiscovery.resolveTarget(
                    duplicateMatches, null, "https://example.test/", "Same"));
    assertEquals(duplicateMatches, ambiguousException.candidates());
  }

  @Test
  void rejectsMalformedTargetResponses() {
    String missingInspector =
        """
        [{"id":"page-1","type":"page","title":"Page","url":"about:blank"}]
        """;
    String duplicateTargets =
        """
        [
          {
            "id":"page-1",
            "type":"page",
            "title":"First",
            "url":"about:blank",
            "devtoolsFrontendUrl":"/first"
          },
          {
            "id":"page-1",
            "type":"page",
            "title":"Second",
            "url":"about:blank",
            "devtoolsFrontendUrl":"/second"
          }
        ]
        """;

    assertThrows(
        DevToolsDiscoveryException.class,
        () -> GrapheneCefDevToolsDiscovery.parsePageTargets(missingInspector, DISCOVERY_URI));
    assertThrows(
        DevToolsDiscoveryException.class,
        () -> GrapheneCefDevToolsDiscovery.parsePageTargets(duplicateTargets, DISCOVERY_URI));
  }

  private static DevToolsPageTarget target(String id, String title) {
    return new DevToolsPageTarget(
        id, title, "https://example.test/", URI.create("http://127.0.0.1:9333/devtools/" + id));
  }
}
