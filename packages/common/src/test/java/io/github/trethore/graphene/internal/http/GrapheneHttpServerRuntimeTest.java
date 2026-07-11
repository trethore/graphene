package io.github.trethore.graphene.internal.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.config.GrapheneHttpConfig;
import io.github.trethore.graphene.api.url.GrapheneAssetUrls;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GrapheneHttpServerRuntimeTest {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

  @TempDir Path tempDir;

  private static TestHttpResponse sendRequest(String method, String url) throws IOException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .timeout(REQUEST_TIMEOUT)
            .build();
    try {
      HttpResponse<String> response =
          HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return new TestHttpResponse(response.statusCode(), response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while performing HTTP request", exception);
    }
  }

  private static TestHttpResponse sendGet(String url) throws IOException {
    return sendRequest("GET", url);
  }

  private static TestHttpResponse sendPost(String url) throws IOException {
    return sendRequest("POST", url);
  }

  private static TestHttpResponse sendHead(String url) throws IOException {
    return sendRequest("HEAD", url);
  }

  private static boolean tryCreateSymbolicLink(Path linkPath, Path targetPath) {
    try {
      Files.createSymbolicLink(linkPath, targetPath);
      return true;
    } catch (UnsupportedOperationException | SecurityException | IOException ignored) {
      return false;
    }
  }

  private static void startAndClose(Map<String, GrapheneHttpConfig> configs) {
    try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(configs)) {
      assertTrue(server.isRunning());
    }
  }

  @Test
  void disabledServerIsStableAndNotRunning() {
    GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.disabled();

    assertFalse(server.isRunning());
    assertEquals("", server.host());
    assertEquals(-1, server.port());
    assertEquals("", server.baseUrl());
    server.close();
  }

  @Test
  void validatesConsumerIdsAndMergedServerSettings() {
    GrapheneHttpConfig firstConfig =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).build();
    GrapheneHttpConfig conflictingConfig =
        GrapheneHttpConfig.builder()
            .bindHost("localhost")
            .randomPortInRange(30_000, 60_000)
            .build();
    Map<String, GrapheneHttpConfig> invalidIdConfigs = Map.of("Invalid Mod", firstConfig);
    Map<String, GrapheneHttpConfig> conflictingConfigs =
        Map.of("mod-a", firstConfig, "mod-b", conflictingConfig);

    assertThrows(IllegalArgumentException.class, () -> startAndClose(invalidIdConfigs));
    assertThrows(IllegalStateException.class, () -> startAndClose(conflictingConfigs));
  }

  @Test
  void suppliesUrlsOnlyWhileServerIsRunning() {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).build();
    GrapheneAssetUrls assetUrls;

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod", config))) {
      assertEquals(
          server.baseUrl() + "/assets/my-mod/web/index.html",
          server.urls().assets("my-mod").url("web/index.html"));
      assetUrls = server.urls().assets("my-mod");
    }

    assertThrows(IllegalStateException.class, () -> assetUrls.url("web/index.html"));
  }

  @Test
  void servesMountedFileSystemResourceWhenConsumerFileRootConfigured() throws IOException {
    Path scriptPath = tempDir.resolve("web/live.js");
    Files.createDirectories(scriptPath.getParent());
    String scriptBody = "console.log('live reload');";
    Files.writeString(scriptPath, scriptBody, StandardCharsets.UTF_8);

    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).fileRoot(tempDir).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendGet(server.baseUrl() + "/mods/my-mod-id/web/live.js");

      assertEquals(200, response.statusCode());
      assertEquals(scriptBody, response.body());
    }
  }

  @Test
  void mountedFileSystemResourceDoesNotOverrideClasspathAssets() throws IOException {
    Path overridePath = tempDir.resolve("assets/grapheneui/example.html");
    Files.createDirectories(overridePath.getParent());
    String overrideBody = "<html><body>filesystem override</body></html>";
    Files.writeString(overridePath, overrideBody, StandardCharsets.UTF_8);

    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).fileRoot(tempDir).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendGet(server.baseUrl() + "/assets/grapheneui/example.html");

      assertEquals(200, response.statusCode());
      assertTrue(response.body().contains("<title>Graphene Test Asset</title>"));
    }
  }

  @Test
  void mountedAssetsPathServesMountedFileSystemResourceBeforeSpaFallback() throws IOException {
    Path stylesheetPath = tempDir.resolve("assets/index.css");
    Files.createDirectories(stylesheetPath.getParent());
    String stylesheetBody = "body { color: red; }";
    Files.writeString(stylesheetPath, stylesheetBody, StandardCharsets.UTF_8);

    Path fallbackPath = tempDir.resolve("index.html");
    Files.writeString(fallbackPath, "<html><body>fallback</body></html>", StandardCharsets.UTF_8);

    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder()
            .randomPortInRange(30_000, 60_000)
            .fileRoot(tempDir)
            .spaFallback("/index.html")
            .build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendGet(server.baseUrl() + "/mods/my-mod-id/assets/index.css");

      assertEquals(200, response.statusCode());
      assertEquals(stylesheetBody, response.body());
    }
  }

  @Test
  void servesClasspathResourceWhenRequestedFromAssetsRoute() throws IOException {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).fileRoot(tempDir).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendGet(server.baseUrl() + "/assets/grapheneui/example.html");

      assertEquals(200, response.statusCode());
      assertTrue(response.body().contains("<title>Graphene Test Asset</title>"));
    }
  }

  @Test
  void servesMountedFileSystemResourceForPostRequests() throws IOException {
    Path htmlPath = tempDir.resolve("submit-target.html");
    Files.createDirectories(htmlPath.getParent());
    String htmlBody = "<html><body>ok</body></html>";
    Files.writeString(htmlPath, htmlBody, StandardCharsets.UTF_8);

    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).fileRoot(tempDir).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendPost(server.baseUrl() + "/mods/my-mod-id/submit-target.html");

      assertEquals(200, response.statusCode());
      assertEquals(htmlBody, response.body());
    }
  }

  @Test
  void servesMountedClasspathResourceFromConsumerNamespaceWhenFileIsMissing() throws IOException {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("grapheneui", config))) {
      TestHttpResponse response = sendGet(server.baseUrl() + "/mods/grapheneui/example.html");

      assertEquals(200, response.statusCode());
      assertTrue(response.body().contains("<title>Graphene Test Asset</title>"));
    }
  }

  @Test
  void isolatesMountedFileRootsPerConsumerMod() throws IOException {
    Path modAPath = tempDir.resolve("mod-a/index.html");
    Files.createDirectories(modAPath.getParent());
    Files.writeString(modAPath, "mod-a", StandardCharsets.UTF_8);

    Path modBPath = tempDir.resolve("mod-b/index.html");
    Files.createDirectories(modBPath.getParent());
    Files.writeString(modBPath, "mod-b", StandardCharsets.UTF_8);

    GrapheneHttpConfig modAConfig =
        GrapheneHttpConfig.builder()
            .randomPortInRange(30_000, 60_000)
            .fileRoot(tempDir.resolve("mod-a"))
            .build();
    GrapheneHttpConfig modBConfig =
        GrapheneHttpConfig.builder()
            .randomPortInRange(30_000, 60_000)
            .fileRoot(tempDir.resolve("mod-b"))
            .build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(
            Map.of(
                "mod-a", modAConfig,
                "mod-b", modBConfig))) {
      TestHttpResponse modAResponse = sendGet(server.baseUrl() + "/mods/mod-a/index.html");
      TestHttpResponse modBResponse = sendGet(server.baseUrl() + "/mods/mod-b/index.html");

      assertEquals(200, modAResponse.statusCode());
      assertEquals("mod-a", modAResponse.body());
      assertEquals(200, modBResponse.statusCode());
      assertEquals("mod-b", modBResponse.body());
    }
  }

  @Test
  void appliesMountedSpaFallbackForPostRequests() throws IOException {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder()
            .randomPortInRange(30_000, 60_000)
            .spaFallback("/assets/grapheneui/example.html")
            .build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendPost(server.baseUrl() + "/mods/my-mod-id/signin");

      assertEquals(200, response.statusCode());
      assertTrue(response.body().contains("<title>Graphene Test Asset</title>"));
    }
  }

  @Test
  void appliesMountedSpaFallbackForHeadRequests() throws IOException {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder()
            .randomPortInRange(30_000, 60_000)
            .spaFallback("/assets/grapheneui/example.html")
            .build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendHead(server.baseUrl() + "/mods/my-mod-id/signin");

      assertEquals(200, response.statusCode());
    }
  }

  @Test
  void rejectsUnsupportedMethods() throws IOException {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response =
          sendRequest("PUT", server.baseUrl() + "/assets/grapheneui/example.html");

      assertEquals(405, response.statusCode());
      assertEquals("Method Not Allowed", response.body());
    }
  }

  @Test
  void rejectsParentTraversalRequestPaths() throws IOException {
    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).fileRoot(tempDir).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response =
          sendGet(server.baseUrl() + "/mods/my-mod-id/%2e%2e/%2e%2e/outside.txt");

      assertEquals(400, response.statusCode());
      assertEquals("Invalid path", response.body());
    }
  }

  @Test
  void doesNotServeSymlinkEscapingMountedFileRoot() throws IOException {
    Path fileRoot = tempDir.resolve("root");
    Files.createDirectories(fileRoot.resolve("web"));

    Path outsideFile = tempDir.resolve("outside.txt");
    Files.writeString(outsideFile, "outside", StandardCharsets.UTF_8);

    Path linkPath = fileRoot.resolve("web/escape.txt");
    boolean symlinkCreated = tryCreateSymbolicLink(linkPath, outsideFile);
    Assumptions.assumeTrue(symlinkCreated, "symbolic links are not available in this environment");

    GrapheneHttpConfig config =
        GrapheneHttpConfig.builder().randomPortInRange(30_000, 60_000).fileRoot(fileRoot).build();

    try (GrapheneHttpServerRuntime server =
        GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
      TestHttpResponse response = sendGet(server.baseUrl() + "/mods/my-mod-id/web/escape.txt");

      assertEquals(404, response.statusCode());
    }
  }

  private record TestHttpResponse(int statusCode, String body) {}
}
