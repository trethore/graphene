package io.github.trethore.graphene.internal.cef;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.trethore.graphene.api.devtools.DevToolsDiscoveryException;
import io.github.trethore.graphene.api.devtools.DevToolsPageTarget;
import io.github.trethore.graphene.api.devtools.DevToolsTargetAmbiguousException;
import io.github.trethore.graphene.api.devtools.DevToolsTargetNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

final class GrapheneCefDevToolsDiscovery {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

  private GrapheneCefDevToolsDiscovery() {}

  static CompletionStage<List<DevToolsPageTarget>> pageTargets(int port) {
    URI discoveryUri = discoveryUri(port);
    HttpRequest request =
        HttpRequest.newBuilder(discoveryUri).GET().timeout(REQUEST_TIMEOUT).build();
    return HTTP_CLIENT
        .sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        .handle(
            (response, failure) -> {
              if (failure != null) {
                throw new DevToolsDiscoveryException(
                    "Failed to query remote DevTools targets from " + discoveryUri, failure);
              }
              if (response.statusCode() != 200) {
                throw new DevToolsDiscoveryException(
                    "Remote DevTools target discovery returned HTTP " + response.statusCode());
              }
              return parsePageTargets(response.body(), discoveryUri);
            });
  }

  static CompletionStage<DevToolsPageTarget> targetFor(
      GrapheneCefBrowserSession session, int port) {
    String sessionUrl = session.currentUrl();
    String sessionTitle = session.currentTitle();
    String boundTargetId = session.devToolsTargetId();
    return pageTargets(port)
        .thenApply(
            targets -> {
              DevToolsPageTarget target =
                  resolveTarget(targets, boundTargetId, sessionUrl, sessionTitle);
              if (boundTargetId == null) {
                session.bindDevToolsTarget(target.id());
              }
              return target;
            });
  }

  static List<DevToolsPageTarget> parsePageTargets(String responseBody, URI discoveryUri) {
    try {
      JsonElement response = JsonParser.parseString(responseBody);
      if (!response.isJsonArray()) {
        throw new DevToolsDiscoveryException(
            "Remote DevTools target discovery response must be a JSON array");
      }
      JsonArray targetArray = response.getAsJsonArray();
      List<DevToolsPageTarget> targets = new ArrayList<>(targetArray.size());
      Set<String> targetIds = new HashSet<>();
      URI discoveryOrigin = discoveryUri.resolve("/");
      for (JsonElement targetElement : targetArray) {
        if (!targetElement.isJsonObject()) {
          throw new DevToolsDiscoveryException(
              "Remote DevTools target discovery entries must be JSON objects");
        }
        JsonObject targetObject = targetElement.getAsJsonObject();
        if (!"page".equals(requiredString(targetObject, "type"))) {
          continue;
        }
        DevToolsPageTarget target =
            new DevToolsPageTarget(
                requiredString(targetObject, "id"),
                requiredStringAllowBlank(targetObject, "title"),
                requiredString(targetObject, "url"),
                inspectorUri(targetObject, discoveryOrigin));
        if (!targetIds.add(target.id())) {
          throw new DevToolsDiscoveryException(
              "Remote DevTools target discovery returned duplicate target id " + target.id());
        }
        targets.add(target);
      }
      return List.copyOf(targets);
    } catch (DevToolsDiscoveryException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new DevToolsDiscoveryException(
          "Failed to parse remote DevTools target discovery response", exception);
    }
  }

  static DevToolsPageTarget resolveTarget(
      List<DevToolsPageTarget> targets,
      String boundTargetId,
      String sessionUrl,
      String sessionTitle) {
    if (boundTargetId != null) {
      return targets.stream()
          .filter(target -> target.id().equals(boundTargetId))
          .findFirst()
          .orElseThrow(() -> new DevToolsTargetNotFoundException(sessionUrl, sessionTitle));
    }

    List<DevToolsPageTarget> urlMatches =
        targets.stream().filter(target -> target.url().equals(sessionUrl)).toList();
    if (urlMatches.isEmpty()) {
      throw new DevToolsTargetNotFoundException(sessionUrl, sessionTitle);
    }
    if (urlMatches.size() == 1) {
      return urlMatches.getFirst();
    }

    if (!sessionTitle.isBlank()) {
      List<DevToolsPageTarget> titleMatches =
          urlMatches.stream().filter(target -> target.title().equals(sessionTitle)).toList();
      if (titleMatches.size() == 1) {
        return titleMatches.getFirst();
      }
      if (titleMatches.size() > 1) {
        throw new DevToolsTargetAmbiguousException(sessionUrl, sessionTitle, titleMatches);
      }
    }
    throw new DevToolsTargetAmbiguousException(sessionUrl, sessionTitle, urlMatches);
  }

  private static URI discoveryUri(int port) {
    if (port <= 0 || port > 65_535) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }
    return URI.create("http://127.0.0.1:" + port + "/json/list");
  }

  private static URI inspectorUri(JsonObject targetObject, URI discoveryOrigin) {
    String inspectorUrl = optionalString(targetObject, "devtoolsFrontendUrlCompat");
    if (inspectorUrl == null || inspectorUrl.isBlank()) {
      inspectorUrl = requiredString(targetObject, "devtoolsFrontendUrl");
    }
    URI inspectorUri = URI.create(inspectorUrl);
    if (inspectorUri.isAbsolute()) {
      return inspectorUri;
    }
    return discoveryOrigin.resolve(inspectorUri);
  }

  private static String optionalString(JsonObject object, String name) {
    JsonElement value = object.get(name);
    if (value == null || value.isJsonNull()) {
      return null;
    }
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new DevToolsDiscoveryException(
          "Remote DevTools target field " + name + " must be a string");
    }
    return value.getAsString();
  }

  private static String requiredString(JsonObject object, String name) {
    String value = requiredStringAllowBlank(object, name);
    if (value.isBlank()) {
      throw new DevToolsDiscoveryException(
          "Remote DevTools target field " + name + " must not be blank");
    }
    return value;
  }

  private static String requiredStringAllowBlank(JsonObject object, String name) {
    String value = optionalString(object, name);
    if (value == null) {
      throw new DevToolsDiscoveryException(
          "Remote DevTools target is missing required field " + name);
    }
    return value;
  }
}
