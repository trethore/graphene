package tytoo.grapheneuidebug.screen;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;
import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;
import tytoo.grapheneuidebug.GrapheneDebugClient;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GrapheneBrowserDebugScreen extends Screen {
    private static final String DEFAULT_URL = GrapheneClasspathUrls.asset(GrapheneDebugClient.ID, "graphene_test/welcome.html");
    private static String lastUrl = DEFAULT_URL;
    private final List<GrapheneBridgeSubscription> bridgeSubscriptions = new ArrayList<>();
    private GrapheneWebViewWidget webViewWidget;
    private EditBox urlBox;
    private Button backButton;
    private Button forwardButton;

    public GrapheneBrowserDebugScreen() {
        super(Component.translatable("screen.graphene-ui-debug.title"));
    }

    private static void rememberLastUrl(String url) {
        lastUrl = url;
    }

    private static String buildEchoResponse(String payloadJson) {
        JsonObject response = new JsonObject();
        response.addProperty("ok", true);
        response.addProperty("kind", "echo");
        response.addProperty("receivedAt", Instant.now().toString());
        response.add("received", parsePayload(payloadJson));
        return response.toString();
    }

    private static String buildSumResponse(String payloadJson) {
        JsonObject response = new JsonObject();
        response.addProperty("kind", "sum");
        response.addProperty("receivedAt", Instant.now().toString());

        try {
            JsonObject payload = parsePayload(payloadJson).getAsJsonObject();
            double left = payload.get("a").getAsDouble();
            double right = payload.get("b").getAsDouble();
            response.addProperty("ok", true);
            response.addProperty("a", left);
            response.addProperty("b", right);
            response.addProperty("result", left + right);
            return response.toString();
        } catch (RuntimeException _) {
            response.addProperty("ok", false);
            response.addProperty("error", "Payload must be a JSON object with numeric fields 'a' and 'b'.");
            return response.toString();
        }
    }

    private static JsonElement parsePayload(String payloadJson) {
        String value = payloadJson == null ? "null" : payloadJson;
        try {
            return JsonParser.parseString(value);
        } catch (RuntimeException _) {
            // Ignore malformed payloads and treat them as null in debug helpers.
            return JsonNull.INSTANCE;
        }
    }

    private void openRemoteDevTools() {
        int debugPort = GrapheneCore.runtime().getRemoteDebuggingPort();
        if (debugPort > 0) {
            Util.getPlatform().openUri(URI.create("http://127.0.0.1:" + debugPort + "/json"));
            emitDevToolsStatus(true, debugPort);
            return;
        }

        emitDevToolsStatus(false, debugPort);
    }

    @Override
    protected void init() {
        clearWidgets();

        int controlsY = 8;
        int controlHeight = 20;
        int webViewY = controlsY + controlHeight + 8;
        int webViewWidth = width - 16;
        int webViewHeight = height - webViewY - 8;

        if (webViewWidget == null) {
            webViewWidget = new GrapheneWebViewWidget(this, 8, webViewY, webViewWidth, webViewHeight, Component.empty(), lastUrl);
        } else {
            webViewWidget.setPosition(8, webViewY);
            webViewWidget.setSize(webViewWidth, webViewHeight);
        }

        configureBridgeHandlers();

        addRenderableWidget(webViewWidget);

        backButton = addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.back"), _ -> webViewWidget.goBack())
                        .bounds(8, controlsY, 26, controlHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.reload"), _ -> webViewWidget.reload())
                        .bounds(38, controlsY, 26, controlHeight)
                        .build()
        );

        forwardButton = addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.forward"), _ -> webViewWidget.goForward())
                        .bounds(68, controlsY, 26, controlHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.devtools"), _ -> openRemoteDevTools())
                        .bounds(98, controlsY, 66, controlHeight)
                        .build()
        );

        urlBox = addRenderableWidget(new EditBox(font, 168, controlsY, width - 176, controlHeight, Component.empty()));
        urlBox.setMaxLength(Integer.MAX_VALUE);
        urlBox.setValue(lastUrl);
    }

    @Override
    public void tick() {
        if (webViewWidget == null) {
            return;
        }

        if (urlBox != null && !urlBox.isFocused()) {
            urlBox.setValue(webViewWidget.currentUrl());
        }

        if (backButton != null) {
            backButton.active = webViewWidget.canGoBack();
        }

        if (forwardButton != null) {
            forwardButton.active = webViewWidget.canGoForward();
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (urlBox != null && urlBox.isFocused() && keyEvent.isConfirmation()) {
            webViewWidget.loadUrl(urlBox.getValue());
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public void onClose() {
        if (webViewWidget != null) {
            rememberLastUrl(webViewWidget.currentUrl());
        }

        clearBridgeSubscriptions();

        super.onClose();
    }

    private void configureBridgeHandlers() {
        clearBridgeSubscriptions();

        GrapheneBridge bridge = webViewWidget.bridge();
        bridgeSubscriptions.add(bridge.onEvent("debug:event", (channel, payloadJson) ->
                GrapheneDebugClient.LOGGER.info("Received bridge event on {}: {}", channel, payloadJson)
        ));
        bridgeSubscriptions.add(bridge.onRequest("debug:echo", (_, payloadJson) ->
                CompletableFuture.completedFuture(buildEchoResponse(payloadJson))
        ));
        bridgeSubscriptions.add(bridge.onRequest("debug:sum", (_, payloadJson) ->
                CompletableFuture.completedFuture(buildSumResponse(payloadJson))
        ));
    }

    private void clearBridgeSubscriptions() {
        for (GrapheneBridgeSubscription bridgeSubscription : bridgeSubscriptions) {
            bridgeSubscription.unsubscribe();
        }

        bridgeSubscriptions.clear();
    }

    private void emitDevToolsStatus(boolean opened, int debugPort) {
        if (webViewWidget == null) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("opened", opened);
        payload.addProperty("debugPort", debugPort);
        payload.addProperty("openedAt", Instant.now().toString());
        webViewWidget.bridge().emit("debug:devtools-status", payload.toString());
    }
}
