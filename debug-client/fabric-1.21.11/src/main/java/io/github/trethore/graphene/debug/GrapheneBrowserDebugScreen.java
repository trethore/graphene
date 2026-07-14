package io.github.trethore.graphene.debug;

import io.github.trethore.graphene.fabric.api.screen.GrapheneScreens;
import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import java.net.URI;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

final class GrapheneBrowserDebugScreen extends Screen {
  private static final GrapheneBrowserDebugScreen INSTANCE = new GrapheneBrowserDebugScreen();
  private static String lastUrl;

  private GrapheneWebViewWidget webView;
  private GrapheneBrowserDebugBridge debugBridge;
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
      debugBridge = new GrapheneBrowserDebugBridge(webView.bridge());
    } else {
      webView.setPosition(8, webViewY);
      webView.setSize(webViewWidth, webViewHeight);
    }
    addRenderableWidget(webView);

    backButton =
        addRenderableWidget(
            browserNavigationButton(8, controlsY, Component.literal("<-"), webView::goBack));
    addRenderableWidget(
        browserNavigationButton(38, controlsY, Component.literal("R"), webView::reload));
    forwardButton =
        addRenderableWidget(
            browserNavigationButton(68, controlsY, Component.literal("->"), webView::goForward));
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
      navigateBrowser(() -> webView.navigate(urlBox.getValue()));
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

  private void openDevTools() {
    GrapheneDebugClient.context()
        .runtime()
        .remoteDebuggingPort()
        .ifPresent(
            port -> Util.getPlatform().openUri(URI.create("http://127.0.0.1:" + port + "/json")));
  }

  private void closePersistentSession() {
    if (debugBridge != null) {
      debugBridge.close();
      debugBridge = null;
    }
    if (webView != null) {
      webView.close();
      webView = null;
    }
  }

  private Button browserNavigationButton(
      int x, int y, Component message, Runnable navigationAction) {
    return new BrowserNavigationButton(x, y, message, ignored -> navigateBrowser(navigationAction));
  }

  private void navigateBrowser(Runnable navigationAction) {
    setFocused(webView);
    navigationAction.run();
  }

  private static String defaultUrl() {
    return GrapheneDebugClient.context().appAssets().url("graphene_test/pages/welcome.html");
  }

  private static void rememberLastUrl(String url) {
    lastUrl = url;
  }

  private static final class BrowserNavigationButton extends Button.Plain {
    private BrowserNavigationButton(int x, int y, Component message, Button.OnPress onPress) {
      super(x, y, 26, 20, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
      return false;
    }
  }
}
