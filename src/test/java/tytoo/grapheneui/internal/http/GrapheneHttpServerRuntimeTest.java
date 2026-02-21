package tytoo.grapheneui.internal.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tytoo.grapheneui.api.GrapheneHttpConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneHttpServerRuntimeTest {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @TempDir
    Path tempDir;

    @Test
    void servesFileSystemResourceWhenFileRootConfigured() throws IOException {
        Path scriptPath = tempDir.resolve("assets/my-mod-id/web/live.js");
        Files.createDirectories(scriptPath.getParent());
        String scriptBody = "console.log('live reload');";
        Files.writeString(scriptPath, scriptBody, StandardCharsets.UTF_8);

        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(config)) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/assets/my-mod-id/web/live.js");

            assertEquals(200, response.statusCode());
            assertEquals(scriptBody, response.body());
        }
    }

    @Test
    void fileSystemResourceTakesPrecedenceOverClasspathResource() throws IOException {
        Path overridePath = tempDir.resolve("assets/graphene-ui/example.html");
        Files.createDirectories(overridePath.getParent());
        String overrideBody = "<html><body>filesystem override</body></html>";
        Files.writeString(overridePath, overrideBody, StandardCharsets.UTF_8);

        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(config)) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/assets/graphene-ui/example.html");

            assertEquals(200, response.statusCode());
            assertEquals(overrideBody, response.body());
        }
    }

    @Test
    void servesClasspathResourceWhenFileIsMissingFromFileRoot() throws IOException {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(config)) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/assets/graphene-ui/example.html");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("<title>Graphene Widget Example</title>"));
        }
    }

    @Test
    void rejectsParentTraversalRequestPaths() throws IOException {
        GrapheneHttpConfig config = GrapheneHttpConfig.builder()
                .randomPortInRange(30_000, 60_000)
                .fileRoot(tempDir)
                .build();

        try (GrapheneHttpServerRuntime server = GrapheneHttpServerRuntime.start(config)) {
            TestHttpResponse response = sendGet(server.baseUrl() + "/%2e%2e/%2e%2e/outside.txt");

            assertEquals(400, response.statusCode());
            assertEquals("Invalid path", response.body());
        }
    }

    private static TestHttpResponse sendGet(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
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

    private record TestHttpResponse(int statusCode, String body) {
    }
}
