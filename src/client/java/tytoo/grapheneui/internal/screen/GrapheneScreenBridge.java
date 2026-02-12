package tytoo.grapheneui.internal.screen;

import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;

import java.util.List;

public interface GrapheneScreenBridge {
    List<GrapheneWebViewWidget> grapheneWebViewWidgets();

    void addGrapheneWebViewWidget(GrapheneWebViewWidget webViewWidget);

    void removeGrapheneWebViewWidget(GrapheneWebViewWidget webViewWidget);

    boolean isGrapheneWebViewAutoCloseEnabled();

    void setGrapheneWebViewAutoCloseEnabled(boolean autoClose);
}
