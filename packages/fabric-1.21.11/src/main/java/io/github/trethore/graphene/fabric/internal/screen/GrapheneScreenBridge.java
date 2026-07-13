package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import java.util.List;

@SuppressWarnings("java:S100")
public interface GrapheneScreenBridge {
  GrapheneScreenState graphene$state();

  default List<GrapheneWebViewWidget> graphene$webViewWidgets() {
    return graphene$state().webViewWidgets();
  }

  default void graphene$addWebViewWidget(GrapheneWebViewWidget widget) {
    graphene$state().addWebViewWidget(widget);
  }

  default void graphene$removeWebViewWidget(GrapheneWebViewWidget widget) {
    graphene$state().removeWebViewWidget(widget);
  }

  default boolean graphene$isWebViewAutoCloseEnabled() {
    return graphene$state().isWebViewAutoCloseEnabled();
  }

  default void graphene$setWebViewAutoCloseEnabled(boolean autoClose) {
    graphene$state().setWebViewAutoCloseEnabled(autoClose);
  }
}
