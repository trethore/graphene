package tytoo.grapheneui.screen;

import tytoo.grapheneui.browser.GrapheneWebViewWidget;

import java.util.List;

@SuppressWarnings({"java:S100", "java:S116"}) // Sonar, this is a mixin scheme, don't complain about method names.
public interface GrapheneScreenBridge {
    List<GrapheneWebViewWidget> grapheneui$getWebViewWidgets();

    void grapheneui$addWebViewWidget(GrapheneWebViewWidget webViewWidget);

    void grapheneui$removeWebViewWidget(GrapheneWebViewWidget webViewWidget);

    boolean grapheneui$isAutoCloseWebViews();

    void grapheneui$setAutoCloseWebViews(boolean autoClose);
}
