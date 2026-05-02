package tytoo.grapheneui.internal.devtools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class GrapheneDevToolsResolver {
    private static final String LOCAL_DEVTOOLS_ORIGIN_PREFIX = "http://127.0.0.1:";
    private static final String DEVTOOLS_LIST_PATH = "/json/list";
    private static final String DEVTOOLS_INSPECTOR_PATH = "/devtools/inspector.html";

    private final HttpClient httpClient;

    public GrapheneDevToolsResolver() {
        this(HttpClient.newHttpClient());
    }

    GrapheneDevToolsResolver(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    private static URI targetListUri(int debugPort) {
        return URI.create(localDevToolsOrigin(debugPort) + DEVTOOLS_LIST_PATH);
    }

    private static URI resolveUriFromTargetList(int debugPort, String targetUrl, HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("DevTools target list request failed with status " + response.statusCode());
        }

        JsonArray targets = JsonParser.parseString(response.body()).getAsJsonArray();
        JsonObject target = selectTarget(targets, targetUrl);
        return buildLocalUri(debugPort, target);
    }

    private static JsonObject selectTarget(JsonArray targets, String targetUrl) {
        JsonObject firstPageTarget = null;

        for (JsonElement targetElement : targets) {
            JsonObject pageTarget = toPageTarget(targetElement);
            if (pageTarget == null) {
                continue;
            }

            firstPageTarget = firstPageTarget == null ? pageTarget : firstPageTarget;
            if (isRequestedTarget(pageTarget, targetUrl)) {
                return pageTarget;
            }
        }

        if (firstPageTarget != null) {
            return firstPageTarget;
        }

        throw new IllegalStateException("No page target is available for remote DevTools");
    }

    private static JsonObject toPageTarget(JsonElement targetElement) {
        if (!targetElement.isJsonObject()) {
            return null;
        }

        JsonObject target = targetElement.getAsJsonObject();
        return "page".equals(readString(target, "type")) ? target : null;
    }

    private static boolean isRequestedTarget(JsonObject target, String targetUrl) {
        return targetUrl != null && targetUrl.equals(readString(target, "url"));
    }

    private static URI buildLocalUri(int debugPort, JsonObject target) {
        String devToolsFrontendUrl = readString(target, "devtoolsFrontendUrl");
        if (isLocalDevToolsFrontendPath(devToolsFrontendUrl)) {
            return URI.create(localDevToolsOrigin(debugPort) + devToolsFrontendUrl);
        }

        String webSocketDebuggerUrl = readString(target, "webSocketDebuggerUrl");
        if (webSocketDebuggerUrl == null || webSocketDebuggerUrl.isBlank()) {
            throw new IllegalStateException("Selected DevTools target does not expose a WebSocket URL");
        }

        URI webSocketUri = URI.create(webSocketDebuggerUrl);
        return URI.create(
                localDevToolsOrigin(debugPort)
                        + DEVTOOLS_INSPECTOR_PATH
                        + "?ws="
                        + webSocketUri.getRawAuthority()
                        + webSocketUri.getRawPath()
        );
    }

    private static String localDevToolsOrigin(int debugPort) {
        return LOCAL_DEVTOOLS_ORIGIN_PREFIX + debugPort;
    }

    private static boolean isLocalDevToolsFrontendPath(String devToolsFrontendUrl) {
        return devToolsFrontendUrl != null
                && devToolsFrontendUrl.startsWith(DEVTOOLS_INSPECTOR_PATH)
                && !devToolsFrontendUrl.startsWith("//");
    }

    private static String readString(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()) {
            return null;
        }

        return object.get(name).getAsString();
    }

    public CompletableFuture<URI> resolveUri(int debugPort, String targetUrl) {
        if (debugPort <= 0) {
            return CompletableFuture.failedFuture(new IllegalStateException("CEF remote debugging is disabled"));
        }

        HttpRequest request = HttpRequest.newBuilder(targetListUri(debugPort))
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> resolveUriFromTargetList(debugPort, targetUrl, response));
    }
}
