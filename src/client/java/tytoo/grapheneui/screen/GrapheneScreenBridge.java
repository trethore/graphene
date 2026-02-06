package tytoo.grapheneui.screen;

import tytoo.grapheneui.browser.GrapheneWebViewWidget;

import java.util.List;

public interface GrapheneScreenBridge {
    List<GrapheneWebViewWidget> grapheneUi$getWebViewWidgets();

    void grapheneUi$addWebViewWidget(GrapheneWebViewWidget webViewWidget);

    void grapheneUi$removeWebViewWidget(GrapheneWebViewWidget webViewWidget);

    boolean grapheneUi$isAutoCloseWebViews();

    void grapheneUi$setAutoCloseWebViews(boolean autoClose);
}
