package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuContext;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import io.github.trethore.graphene.fabric.internal.util.MinecraftReferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class GrapheneScreenState {
  private final List<GrapheneWebViewWidget> webViewWidgets = new ArrayList<>();
  private GrapheneContextMenuOverlay contextMenu;
  private GrapheneWebViewWidget contextMenuOwner;
  private int suppressedContextMenuButton = -1;
  private boolean autoCloseWebViews = true;
  private boolean removalListenerRegistered;

  void attach(Screen screen) {
    if (removalListenerRegistered) {
      return;
    }
    removalListenerRegistered = true;
    ScreenEvents.remove(screen).register(removedScreen -> dismissContextMenu());
  }

  List<GrapheneWebViewWidget> webViewWidgets() {
    return webViewWidgets;
  }

  void addWebViewWidget(GrapheneWebViewWidget widget) {
    webViewWidgets.add(widget);
  }

  void removeWebViewWidget(GrapheneWebViewWidget widget) {
    if (contextMenuOwner == widget) {
      closeContextMenu();
    }
    webViewWidgets.remove(widget);
  }

  boolean isWebViewAutoCloseEnabled() {
    return autoCloseWebViews;
  }

  void setWebViewAutoCloseEnabled(boolean autoClose) {
    autoCloseWebViews = autoClose;
  }

  public void closeWebViews() {
    closeContextMenu();
    if (!autoCloseWebViews) {
      return;
    }
    List<GrapheneWebViewWidget> widgetsToClose = new ArrayList<>(webViewWidgets);
    widgetsToClose.forEach(GrapheneWebViewWidget::close);
    webViewWidgets.clear();
  }

  public void resizeWebViews() {
    closeContextMenu();
    webViewWidgets.forEach(GrapheneWebViewWidget::handleScreenResize);
  }

  @SuppressWarnings("resource")
  CompletionStage<BrowserContextMenuPresenter.Result> showContextMenu(
      Screen screen, BrowserContextMenuPresenter.Request request) {
    closeContextMenu();
    BrowserContextMenuPresenter.Request validatedRequest =
        Objects.requireNonNull(request, "request");
    BrowserContextMenuContext context = validatedRequest.context();
    GrapheneWebViewWidget widget =
        webViewWidgets.stream()
            .filter(candidate -> candidate.surface().browser() == context.session())
            .findFirst()
            .orElse(null);
    if (widget == null) {
      return CompletableFuture.completedFuture(BrowserContextMenuPresenter.Result.cancel());
    }
    int anchorX =
        widget.getX()
            + Math.round(
                (float) context.position().x()
                    * widget.getWidth()
                    / widget.surface().resolutionWidth());
    int anchorY =
        widget.getY()
            + Math.round(
                (float) context.position().y()
                    * widget.getHeight()
                    / widget.surface().resolutionHeight());
    GrapheneContextMenuOverlay overlay =
        new GrapheneContextMenuOverlay(
            validatedRequest,
            MinecraftReferences.font(),
            anchorX,
            anchorY,
            screen.width,
            screen.height);
    contextMenu = overlay;
    contextMenuOwner = widget;
    overlay.completion().whenComplete((ignoredResult, ignoredFailure) -> clearContextMenu(overlay));
    return overlay.completion();
  }

  void renderContextMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    GrapheneContextMenuOverlay activeMenu = contextMenu;
    if (activeMenu != null) {
      activeMenu.render(graphics, mouseX, mouseY);
    }
  }

  boolean handleContextMenuClick(MouseButtonEvent event) {
    GrapheneContextMenuOverlay activeMenu = contextMenu;
    if (activeMenu == null) {
      return false;
    }
    suppressedContextMenuButton = event.button();
    return activeMenu.mouseClicked(event);
  }

  boolean handleContextMenuRelease(MouseButtonEvent event) {
    GrapheneContextMenuOverlay activeMenu = contextMenu;
    if (event.button() == suppressedContextMenuButton) {
      suppressedContextMenuButton = -1;
      return true;
    }
    return activeMenu != null;
  }

  boolean handleContextMenuKey(KeyEvent event) {
    GrapheneContextMenuOverlay activeMenu = contextMenu;
    if (activeMenu == null) {
      return false;
    }
    activeMenu.keyPressed(event);
    return true;
  }

  boolean isContextMenuOpen() {
    return contextMenu != null;
  }

  private void dismissContextMenu() {
    closeContextMenu();
  }

  private void closeContextMenu() {
    GrapheneContextMenuOverlay activeMenu = contextMenu;
    contextMenu = null;
    contextMenuOwner = null;
    suppressedContextMenuButton = -1;
    if (activeMenu != null) {
      activeMenu.cancel();
    }
  }

  private void clearContextMenu(GrapheneContextMenuOverlay completedMenu) {
    if (contextMenu == completedMenu) {
      contextMenu = null;
      contextMenuOwner = null;
    }
  }
}
