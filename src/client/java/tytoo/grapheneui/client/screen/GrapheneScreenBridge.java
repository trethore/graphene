package tytoo.grapheneui.client.screen;

import tytoo.grapheneui.client.browser.GrapheneWebViewWidget;

import java.util.List;

public interface GrapheneScreenBridge {
    List<GrapheneWebViewWidget> grapheneUi$getWebViewWidgets();

    void grapheneUi$addWebViewWidget(GrapheneWebViewWidget webViewWidget);

    void grapheneUi$removeWebViewWidget(GrapheneWebViewWidget webViewWidget);

    boolean grapheneUi$isAutoCloseWebViews();

    void grapheneUi$setAutoCloseWebViews(boolean autoClose);
}
