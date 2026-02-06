package tytoo.grapheneuidebug.debug.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import tytoo.grapheneui.client.browser.GrapheneWebViewWidget;
import tytoo.grapheneui.client.cef.GrapheneCefRuntime;

import java.net.URI;

public final class GrapheneBrowserDebugScreen extends Screen {
    private static final String DEFAULT_URL = "classpath://assets/graphene-ui/graphene_test/welcome.html";
    private static String lastUrl = DEFAULT_URL;

    private GrapheneWebViewWidget webViewWidget;
    private EditBox urlBox;
    private Button backButton;
    private Button reloadButton;
    private Button forwardButton;

    public GrapheneBrowserDebugScreen() {
        super(Component.translatable("screen.graphene-ui-debug.title"));
    }

    private static void openRemoteDevTools() {
        int debugPort = GrapheneCefRuntime.getRemoteDebuggingPort();
        if (debugPort > 0) {
            Util.getPlatform().openUri(URI.create("http://127.0.0.1:" + debugPort));
        }
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

        addRenderableWidget(webViewWidget);

        backButton = addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.back"), button -> webViewWidget.goBack())
                        .bounds(8, controlsY, 26, controlHeight)
                        .build()
        );

        reloadButton = addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.reload"), button -> webViewWidget.reload())
                        .bounds(38, controlsY, 26, controlHeight)
                        .build()
        );

        forwardButton = addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.forward"), button -> webViewWidget.goForward())
                        .bounds(68, controlsY, 26, controlHeight)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.translatable("screen.graphene-ui-debug.devtools"), button -> openRemoteDevTools())
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
            urlBox.setValue(webViewWidget.getBrowser().getURL());
        }

        if (backButton != null) {
            backButton.active = webViewWidget.getBrowser().canGoBack();
        }

        if (forwardButton != null) {
            forwardButton.active = webViewWidget.getBrowser().canGoForward();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (urlBox != null && urlBox.isFocused() && keyEvent.isConfirmation()) {
            webViewWidget.loadUrl(urlBox.getValue());
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public void onClose() {
        if (webViewWidget != null) {
            lastUrl = webViewWidget.getBrowser().getURL();
        }

        super.onClose();
    }
}
