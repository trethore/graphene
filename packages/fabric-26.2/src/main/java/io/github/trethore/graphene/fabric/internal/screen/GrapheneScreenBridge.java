package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import java.util.List;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

@SuppressWarnings("java:S100")
public interface GrapheneScreenBridge {
  GrapheneScreenState graphene$state();

  default List<GrapheneWebViewWidget> graphene$webViewWidgets() {
    return graphene$state().webViewWidgets();
  }

  default void graphene$addWebViewWidget(GrapheneWebViewWidget widget) {
    GrapheneScreenState state = graphene$state();
    state.attach((Screen) this);
    state.addWebViewWidget(widget);
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

  default CompletionStage<BrowserContextMenuPresenter.Result> graphene$showContextMenu(
      BrowserContextMenuPresenter.Request request) {
    return graphene$state().showContextMenu((Screen) this, request);
  }

  default void graphene$renderContextMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    graphene$state().renderContextMenu(graphics, mouseX, mouseY);
  }

  default boolean graphene$handleContextMenuClick(MouseButtonEvent event) {
    return graphene$state().handleContextMenuClick(event);
  }

  default boolean graphene$handleContextMenuRelease(MouseButtonEvent event) {
    return graphene$state().handleContextMenuRelease(event);
  }

  default boolean graphene$handleContextMenuKey(KeyEvent event) {
    return graphene$state().handleContextMenuKey(event);
  }

  default boolean graphene$isContextMenuOpen() {
    return graphene$state().isContextMenuOpen();
  }
}
