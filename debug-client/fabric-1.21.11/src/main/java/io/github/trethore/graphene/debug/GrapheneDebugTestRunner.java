package io.github.trethore.graphene.debug;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.fabric.api.surface.BrowserSurface;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

final class GrapheneDebugTestRunner {
  private static final String PASSED_PROPERTY = "passed";

  private GrapheneDebugTestRunner() {}

  static CompletableFuture<String> runAllTestsAsJson() {
    Instant started = Instant.now();
    return onClientThread(GrapheneDebugTestRunner::runTests)
        .handle(
            (results, failure) -> {
              JsonObject report = new JsonObject();
              report.addProperty("startedAt", started.toString());
              report.addProperty("durationMs", Duration.between(started, Instant.now()).toMillis());
              JsonArray entries = new JsonArray();
              if (results != null) {
                results.forEach(entries::add);
              }
              report.add("results", entries);
              report.addProperty(
                  "ok",
                  failure == null
                      && results != null
                      && results.stream().allMatch(GrapheneDebugTestRunner::passed));
              if (failure != null) {
                report.addProperty("error", failure.getMessage());
              }
              return report.toString();
            });
  }

  private static List<JsonObject> runTests() {
    List<JsonObject> results = new ArrayList<>();
    results.add(
        test(
            "runtime",
            () ->
                require(
                    GrapheneDebugClient.context().runtime().isInitialized(),
                    "Runtime is not initialized")));
    results.add(
        test(
            "browser-surface",
            () -> {
              try (BrowserSurface surface =
                  BrowserSurface.builder(GrapheneDebugClient.context())
                      .url("about:blank")
                      .size(16, 16)
                      .build()) {
                surface.resize(24, 24);
                surface.setResolution(32, 32);
                surface.useAutoResolution();
              }
            }));
    results.add(
        test(
            "browser-navigation",
            () -> {
              try (BrowserSurface surface =
                  BrowserSurface.builder(GrapheneDebugClient.context())
                      .url("about:blank")
                      .size(16, 16)
                      .build()) {
                BrowserSession browser = surface.browser();
                require(!browser.isClosed(), "Browser closed immediately");
                browser.reload();
              }
            }));
    return results;
  }

  private static JsonObject test(String name, Runnable action) {
    JsonObject result = new JsonObject();
    result.addProperty("name", name);
    try {
      action.run();
      result.addProperty(PASSED_PROPERTY, true);
    } catch (RuntimeException exception) {
      result.addProperty(PASSED_PROPERTY, false);
      result.addProperty("error", exception.getMessage());
    }
    return result;
  }

  private static CompletableFuture<List<JsonObject>> onClientThread(
      java.util.function.Supplier<List<JsonObject>> action) {
    CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
    Minecraft.getInstance()
        .execute(
            () -> {
              try {
                future.complete(action.get());
              } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
              }
            });
    return future;
  }

  private static boolean passed(JsonObject result) {
    return result.get(PASSED_PROPERTY).getAsBoolean();
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }
}
