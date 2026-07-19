package io.github.trethore.graphene.internal.cef;

import com.google.gson.JsonObject;
import io.github.trethore.graphene.api.browser.BrowserOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.cef.browser.CefDevToolsClient;

final class GrapheneCefBrowserOptions {
  private static final String BACKGROUND_METHOD = "Emulation.setDefaultBackgroundColorOverride";
  private static final String JAVASCRIPT_METHOD = "Emulation.setScriptExecutionDisabled";

  private GrapheneCefBrowserOptions() {}

  static boolean requiresInitialization(BrowserOptions options) {
    BrowserOptions validatedOptions = Objects.requireNonNull(options, "options");
    return requiresBackgroundOverride(validatedOptions)
        || requiresJavaScriptOverride(validatedOptions);
  }

  static CompletableFuture<Void> apply(CefDevToolsClient devToolsClient, BrowserOptions options) {
    CefDevToolsClient validatedClient = Objects.requireNonNull(devToolsClient, "devToolsClient");
    CompletableFuture<?>[] commandFutures =
        commands(options).stream()
            .map(
                command ->
                    validatedClient.executeDevToolsMethod(command.method(), command.parameters()))
            .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(commandFutures);
  }

  static List<DevToolsCommand> commands(BrowserOptions options) {
    BrowserOptions validatedOptions = Objects.requireNonNull(options, "options");
    List<DevToolsCommand> commands = new ArrayList<>(2);
    if (requiresBackgroundOverride(validatedOptions)) {
      commands.add(backgroundCommand(validatedOptions.backgroundColor()));
    }
    if (requiresJavaScriptOverride(validatedOptions)) {
      commands.add(new DevToolsCommand(JAVASCRIPT_METHOD, javascriptParameters()));
    }
    return List.copyOf(commands);
  }

  private static boolean requiresBackgroundOverride(BrowserOptions options) {
    return !options.transparent();
  }

  private static boolean requiresJavaScriptOverride(BrowserOptions options) {
    return !options.javascriptEnabled();
  }

  private static DevToolsCommand backgroundCommand(int backgroundColor) {
    JsonObject color = new JsonObject();
    color.addProperty("r", backgroundColor >>> 16 & 0xFF);
    color.addProperty("g", backgroundColor >>> 8 & 0xFF);
    color.addProperty("b", backgroundColor & 0xFF);
    color.addProperty("a", 1);
    JsonObject parameters = new JsonObject();
    parameters.add("color", color);
    return new DevToolsCommand(BACKGROUND_METHOD, parameters.toString());
  }

  private static String javascriptParameters() {
    JsonObject parameters = new JsonObject();
    parameters.addProperty("value", true);
    return parameters.toString();
  }

  record DevToolsCommand(String method, String parameters) {
    DevToolsCommand {
      Objects.requireNonNull(method, "method");
      Objects.requireNonNull(parameters, "parameters");
    }
  }
}
