package io.github.trethore.graphene.debug;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeSubscription;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class GrapheneBrowserDebugBridge implements AutoCloseable {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

  private final GrapheneBridge bridge;
  private final List<GrapheneBridgeSubscription> subscriptions = new ArrayList<>();

  GrapheneBrowserDebugBridge(GrapheneBridge bridge) {
    this.bridge = bridge;
    registerHandlers();
  }

  @Override
  public void close() {
    subscriptions.forEach(GrapheneBridgeSubscription::unsubscribe);
    subscriptions.clear();
  }

  private void registerHandlers() {
    subscriptions.add(
        bridge.onRequest(
            "debug:echo",
            (channel, payload) -> CompletableFuture.completedFuture(echoResponse(payload))));
    subscriptions.add(
        bridge.onRequest(
            "debug:sum",
            (channel, payload) -> CompletableFuture.completedFuture(sumResponse(payload))));
    subscriptions.add(
        bridge.onRequest(
            "debug:tests:run", (channel, payload) -> GrapheneDebugTestRunner.runAllTestsAsJson()));
    subscriptions.add(
        bridge.onRequest(
            "debug:bridge:trigger-java-to-js", (channel, payload) -> javaToJsRoundTrip(payload)));
  }

  private CompletableFuture<String> javaToJsRoundTrip(String payload) {
    JsonObject event = new JsonObject();
    event.addProperty("sentAt", Instant.now().toString());
    event.add("payload", parse(payload));
    bridge.emit("debug:bridge:java-event", event.toString());
    return bridge
        .request("debug:bridge:java-request", event.toString(), REQUEST_TIMEOUT)
        .thenApply(
            response -> {
              JsonObject result = new JsonObject();
              result.addProperty("ok", true);
              result.add("response", parse(response));
              return result.toString();
            });
  }

  private static String echoResponse(String payload) {
    JsonObject response = new JsonObject();
    response.addProperty("ok", true);
    response.add("received", parse(payload));
    return response.toString();
  }

  private static String sumResponse(String payload) {
    JsonObject response = new JsonObject();
    try {
      JsonObject request = parse(payload).getAsJsonObject();
      response.addProperty("ok", true);
      response.addProperty(
          "result", request.get("a").getAsDouble() + request.get("b").getAsDouble());
    } catch (RuntimeException exception) {
      response.addProperty("ok", false);
      response.addProperty("error", "Expected numeric fields 'a' and 'b'");
    }
    return response.toString();
  }

  private static JsonElement parse(String json) {
    try {
      return JsonParser.parseString(json == null ? "null" : json);
    } catch (RuntimeException exception) {
      return JsonNull.INSTANCE;
    }
  }
}
