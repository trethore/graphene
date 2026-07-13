package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import java.util.ArrayList;
import java.util.List;

public final class GrapheneScreenState {
  private final List<GrapheneWebViewWidget> webViewWidgets = new ArrayList<>();
  private boolean autoCloseWebViews = true;

  public List<GrapheneWebViewWidget> webViewWidgets() {
    return webViewWidgets;
  }

  public void addWebViewWidget(GrapheneWebViewWidget widget) {
    webViewWidgets.add(widget);
  }

  public void removeWebViewWidget(GrapheneWebViewWidget widget) {
    webViewWidgets.remove(widget);
  }

  public boolean isWebViewAutoCloseEnabled() {
    return autoCloseWebViews;
  }

  public void setWebViewAutoCloseEnabled(boolean autoClose) {
    autoCloseWebViews = autoClose;
  }

  public void closeWebViews() {
    if (!autoCloseWebViews) {
      return;
    }
    List<GrapheneWebViewWidget> widgetsToClose = new ArrayList<>(webViewWidgets);
    widgetsToClose.forEach(GrapheneWebViewWidget::close);
    webViewWidgets.clear();
  }

  public void resizeWebViews() {
    webViewWidgets.forEach(GrapheneWebViewWidget::handleScreenResize);
  }
}
