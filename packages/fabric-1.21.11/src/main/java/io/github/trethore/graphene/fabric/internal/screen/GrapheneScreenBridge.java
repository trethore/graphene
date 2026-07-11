package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import java.util.List;

@SuppressWarnings("java:S100")
public interface GrapheneScreenBridge {
  List<GrapheneWebViewWidget> graphene$webViewWidgets();

  void graphene$addWebViewWidget(GrapheneWebViewWidget widget);

  void graphene$removeWebViewWidget(GrapheneWebViewWidget widget);

  boolean graphene$isWebViewAutoCloseEnabled();

  void graphene$setWebViewAutoCloseEnabled(boolean autoClose);
}
