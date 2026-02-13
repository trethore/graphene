package tytoo.grapheneui.internal.screen;

import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;

import java.util.List;

@SuppressWarnings("java:S100") // Yes, this is a mixin bridge.
public interface GrapheneScreenBridge {
    List<GrapheneWebViewWidget> graphene$webViewWidgets();

    void graphene$addWebViewWidget(GrapheneWebViewWidget webViewWidget);

    void graphene$removeWebViewWidget(GrapheneWebViewWidget webViewWidget);

    boolean graphene$isWebViewAutoCloseEnabled();

    void graphene$setWebViewAutoCloseEnabled(boolean autoClose);
}
