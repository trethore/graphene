package tytoo.grapheneui.screen;

import tytoo.grapheneui.browser.GrapheneWebViewWidget;

import java.util.List;

public interface GrapheneScreenBridge {
    List<GrapheneWebViewWidget> grapheneWebViewWidgets();

    void addGrapheneWebViewWidget(GrapheneWebViewWidget webViewWidget);

    void removeGrapheneWebViewWidget(GrapheneWebViewWidget webViewWidget);

    boolean isGrapheneWebViewAutoCloseEnabled();

    void setGrapheneWebViewAutoCloseEnabled(boolean autoClose);
}
