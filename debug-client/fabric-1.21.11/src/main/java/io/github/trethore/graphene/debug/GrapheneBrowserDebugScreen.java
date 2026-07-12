package io.github.trethore.graphene.debug;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeSubscription;
import io.github.trethore.graphene.fabric.api.screen.GrapheneScreens;
import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

final class GrapheneBrowserDebugScreen extends Screen {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
  private static final GrapheneBrowserDebugScreen INSTANCE = new GrapheneBrowserDebugScreen();
  private static String lastUrl;

  private final List<GrapheneBridgeSubscription> subscriptions = new ArrayList<>();

  private GrapheneWebViewWidget webView;
  private EditBox urlBox;
  private Button backButton;
  private Button forwardButton;

  GrapheneBrowserDebugScreen() {
    super(Component.translatable("screen.grapheneui-debug.title"));
    GrapheneScreens.setWebViewAutoCloseEnabled(this, false);
  }

  static GrapheneBrowserDebugScreen instance() {
    return INSTANCE;
  }

  static void closeSession() {
    INSTANCE.closePersistentSession();
  }

  @Override
  protected void init() {
    clearWidgets();
    int controlsY = 8;
    int webViewY = 36;
    int webViewWidth = Math.max(1, width - 16);
    int webViewHeight = Math.max(1, height - webViewY - 8);
    String initialUrl = lastUrl == null ? defaultUrl() : lastUrl;

    if (webView == null) {
      webView =
          new GrapheneWebViewWidget(
              GrapheneDebugClient.context(),
              this,
              8,
              webViewY,
              webViewWidth,
              webViewHeight,
              Component.empty(),
              initialUrl);
      configureBridge();
    } else {
      webView.setPosition(8, webViewY);
      webView.setSize(webViewWidth, webViewHeight);
    }
    addRenderableWidget(webView);

    backButton =
        addRenderableWidget(
            Button.builder(Component.literal("<-"), ignored -> webView.goBack())
                .bounds(8, controlsY, 26, 20)
                .build());
    addRenderableWidget(
        Button.builder(Component.literal("R"), ignored -> webView.reload())
            .bounds(38, controlsY, 26, 20)
            .build());
    forwardButton =
        addRenderableWidget(
            Button.builder(Component.literal("->"), ignored -> webView.goForward())
                .bounds(68, controlsY, 26, 20)
                .build());
    addRenderableWidget(
        Button.builder(Component.literal("DevTools"), ignored -> openDevTools())
            .bounds(98, controlsY, 66, 20)
            .build());
    urlBox =
        addRenderableWidget(
            new EditBox(font, 168, controlsY, Math.max(1, width - 176), 20, Component.empty()));
    urlBox.setMaxLength(Integer.MAX_VALUE);
    urlBox.setValue(initialUrl);
    setFocused(webView);
  }

  @Override
  public void tick() {
    if (webView == null) {
      return;
    }
    if (urlBox != null && !urlBox.isFocused()) {
      urlBox.setValue(webView.currentUrl());
    }
    backButton.active = webView.canGoBack();
    forwardButton.active = webView.canGoForward();
  }

  @Override
  public boolean keyPressed(@NonNull KeyEvent event) {
    if (urlBox != null && urlBox.isFocused() && event.isConfirmation()) {
      webView.navigate(urlBox.getValue());
      setFocused(webView);
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  public void onClose() {
    if (webView != null) {
      rememberLastUrl(webView.currentUrl());
    }
    super.onClose();
  }

  private void configureBridge() {
    GrapheneBridge bridge = webView.bridge();
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
        bridge.onRequest("debug:clipboard:copy", (channel, payload) -> copyToClipboard(payload)));
    subscriptions.add(
        bridge.onRequest(
            "debug:bridge:trigger-java-to-js", (channel, payload) -> javaToJsRoundTrip(payload)));
  }

  private CompletableFuture<String> javaToJsRoundTrip(String payload) {
    GrapheneBridge bridge = webView.bridge();
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

  private CompletableFuture<String> copyToClipboard(String payload) {
    CompletableFuture<String> result = new CompletableFuture<>();
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.execute(
        () -> {
          try {
            JsonObject request = parse(payload).getAsJsonObject();
            minecraft.keyboardHandler.setClipboard(request.get("text").getAsString());
            result.complete("{\"ok\":true}");
          } catch (RuntimeException exception) {
            result.complete("{\"ok\":false}");
          }
        });
    return result;
  }

  private void openDevTools() {
    GrapheneDebugClient.context()
        .runtime()
        .remoteDebuggingPort()
        .ifPresent(
            port -> Util.getPlatform().openUri(URI.create("http://127.0.0.1:" + port + "/json")));
  }

  private void closePersistentSession() {
    subscriptions.forEach(GrapheneBridgeSubscription::unsubscribe);
    subscriptions.clear();
    if (webView != null) {
      webView.close();
      webView = null;
    }
  }

  private static String defaultUrl() {
    return GrapheneDebugClient.context().appAssets().url("graphene_test/pages/welcome.html");
  }

  private static void rememberLastUrl(String url) {
    lastUrl = url;
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
