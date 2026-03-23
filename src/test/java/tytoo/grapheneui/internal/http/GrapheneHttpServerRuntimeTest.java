package tytoo.grapheneui.internal.http;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneHttpServerRuntimeTest {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @TempDir
    Path tempDir;

    private static TestHttpResponse sendRequest(String method, String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .timeout(REQUEST_TIMEOUT)
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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

    @Test
    void servesMountedFileSystemResourceWhenConsumerFileRootConfigured() throws IOException {
        Path scriptPath = tempDir.resolve("web/live.js");
        Files.createDirectories(scriptPath.getParent());
        String scriptBody = "console.log('live reload');";
        Files.writeString(scriptPath, scriptBody, StandardCharsets.UTF_8);

        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/mods/my-mod-id/web/live.js");

            assertEquals(200, response.statusCode());
            assertEquals(scriptBody, response.body());
        }
    }

    @Test
    void mountedFileSystemResourceDoesNotOverrideClasspathAssets() throws IOException {
        Path overridePath = tempDir.resolve("assets/graphene-ui/example.html");
        Files.createDirectories(overridePath.getParent());
        String overrideBody = "<html><body>filesystem override</body></html>";
        Files.writeString(overridePath, overrideBody, StandardCharsets.UTF_8);

        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/assets/graphene-ui/example.html");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("<title>Graphene Widget Example</title>"));
        }
    }

    @Test
    void mountedAssetsPathDoesNotOverrideClasspathAssets() throws IOException {
        Path overridePath = tempDir.resolve("assets/graphene-ui/example.html");
        Files.createDirectories(overridePath.getParent());
        Files.writeString(overridePath, "<html><body>filesystem override</body></html>", StandardCharsets.UTF_8);

        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/mods/my-mod-id/assets/graphene-ui/example.html");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("<title>Graphene Widget Example</title>"));
        }
    }

    @Test
    void servesClasspathResourceWhenRequestedFromAssetsRoute() throws IOException {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/assets/graphene-ui/example.html");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("<title>Graphene Widget Example</title>"));
        }
    }

    @Test
    void servesMountedFileSystemResourceForPostRequests() throws IOException {
        Path htmlPath = tempDir.resolve("submit-target.html");
        Files.createDirectories(htmlPath.getParent());
        String htmlBody = "<html><body>ok</body></html>";
        Files.writeString(htmlPath, htmlBody, StandardCharsets.UTF_8);

        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendPost(server.baseUrl() + "/mods/my-mod-id/submit-target.html");

            assertEquals(200, response.statusCode());
            assertEquals(htmlBody, response.body());
        }
    }

    @Test
    void servesMountedClasspathResourceFromConsumerNamespaceWhenFileIsMissing() throws IOException {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("graphene-ui", config))) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/mods/graphene-ui/example.html");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("<title>Graphene Widget Example</title>"));
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

        GrapheneHttpConfig modAConfig = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir.resolve("mod-a"))
                .build();
        GrapheneHttpConfig modBConfig = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir.resolve("mod-b"))
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of(
                "mod-a", modAConfig,
                "mod-b", modBConfig
        ))) {
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
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .spaFallback("/assets/graphene-ui/example.html")
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendPost(server.baseUrl() + "/mods/my-mod-id/signin");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("<title>Graphene Widget Example</title>"));
        }
    }

    @Test
    void appliesMountedSpaFallbackForHeadRequests() throws IOException {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .spaFallback("/assets/graphene-ui/example.html")
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendHead(server.baseUrl() + "/mods/my-mod-id/signin");

            assertEquals(200, response.statusCode());
        }
    }

    @Test
    void rejectsUnsupportedMethods() throws IOException {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendRequest("PUT", server.baseUrl() + "/assets/graphene-ui/example.html");

            assertEquals(405, response.statusCode());
            assertEquals("Method Not Allowed", response.body());
        }
    }

    @Test
    void rejectsParentTraversalRequestPaths() throws IOException {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/mods/my-mod-id/%2e%2e/%2e%2e/outside.txt");

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

        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(fileRoot)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(Map.of("my-mod-id", config))) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/mods/my-mod-id/web/escape.txt");

            assertEquals(404, response.statusCode());
        }
    }

    private record TestHttpResponse(int statusCode, String body) {
    }
}
