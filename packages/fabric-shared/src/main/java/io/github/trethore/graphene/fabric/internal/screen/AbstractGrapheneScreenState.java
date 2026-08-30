package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuContext;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import io.github.trethore.graphene.minecraft.internal.util.MinecraftReferences;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

abstract class AbstractGrapheneScreenState {
    private final GrapheneScreenWebViewRegistry webViews = new GrapheneScreenWebViewRegistry();
    private GrapheneContextMenuOverlaySupport contextMenu;
    private GrapheneWebViewWidget contextMenuOwner;
    private int suppressedContextMenuButton = -1;
    private boolean removalListenerRegistered;

    final void attach(Screen screen) {
        if (removalListenerRegistered) {
            return;
        }
        removalListenerRegistered = true;
        ScreenEvents.remove(screen).register(ignoredRemovedScreen -> closeContextMenu());
    }

    final List<GrapheneWebViewWidget> webViewWidgets() {
        return webViews.widgets();
    }

    final void addWebViewWidget(GrapheneWebViewWidget widget) {
        webViews.add(widget);
    }

    final void removeWebViewWidget(GrapheneWebViewWidget widget) {
        if (contextMenuOwner == widget) {
            closeContextMenu();
        }
        webViews.remove(widget);
    }

    final boolean isWebViewAutoCloseEnabled() {
        return webViews.autoClose();
    }

    final void setWebViewAutoCloseEnabled(boolean autoClose) {
        webViews.setAutoClose(autoClose);
    }

    public final void closeWebViews() {
        closeContextMenu();
        webViews.closeAll();
    }

    public final void resizeWebViews() {
        closeContextMenu();
        webViews.resizeAll();
    }

    @SuppressWarnings("resource")
    final CompletionStage<BrowserContextMenuPresenter.Result> showContextMenu(
            Screen screen, BrowserContextMenuPresenter.Request request) {
        closeContextMenu();
        BrowserContextMenuPresenter.Request validatedRequest = Objects.requireNonNull(request, "request");
        BrowserContextMenuContext context = validatedRequest.context();
        GrapheneWebViewWidget widget = webViews.widgets().stream()
                .filter(candidate -> candidate.surface().browser() == context.session())
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return CompletableFuture.completedFuture(BrowserContextMenuPresenter.Result.cancel());
        }
        int anchorX = widget.getX()
                + Math.round((float) context.position().x()
                        * widget.getWidth()
                        / widget.surface().resolutionWidth());
        int anchorY = widget.getY()
                + Math.round((float) context.position().y()
                        * widget.getHeight()
                        / widget.surface().resolutionHeight());
        GrapheneContextMenuOverlaySupport overlay = createContextMenuOverlay(
                validatedRequest, MinecraftReferences.font(), anchorX, anchorY, screen.width, screen.height);
        contextMenu = overlay;
        contextMenuOwner = widget;
        overlay.completion().whenComplete((ignoredResult, ignoredFailure) -> clearContextMenu(overlay));
        return overlay.completion();
    }

    final boolean handleContextMenuClick(MouseButtonEvent event) {
        GrapheneContextMenuOverlaySupport activeMenu = contextMenu;
        if (activeMenu == null) {
            return false;
        }
        suppressedContextMenuButton = event.button();
        return activeMenu.mouseClicked(event);
    }

    final boolean handleContextMenuRelease(MouseButtonEvent event) {
        GrapheneContextMenuOverlaySupport activeMenu = contextMenu;
        if (event.button() == suppressedContextMenuButton) {
            suppressedContextMenuButton = -1;
            return true;
        }
        return activeMenu != null;
    }

    final boolean handleContextMenuKey(KeyEvent event) {
        GrapheneContextMenuOverlaySupport activeMenu = contextMenu;
        if (activeMenu == null) {
            return false;
        }
        activeMenu.keyPressed(event);
        return true;
    }

    final boolean isContextMenuOpen() {
        return contextMenu != null;
    }

    final void renderContextMenu(GrapheneGuiGraphics graphics, int mouseX, int mouseY) {
        GrapheneContextMenuOverlaySupport activeMenu = contextMenu;
        if (activeMenu != null) {
            activeMenu.render(graphics, mouseX, mouseY);
        }
    }

    private static GrapheneContextMenuOverlaySupport createContextMenuOverlay(
            BrowserContextMenuPresenter.Request request,
            Font font,
            int anchorX,
            int anchorY,
            int screenWidth,
            int screenHeight) {
        return new GrapheneContextMenuOverlay(request, font, anchorX, anchorY, screenWidth, screenHeight);
    }

    private void closeContextMenu() {
        GrapheneContextMenuOverlaySupport activeMenu = contextMenu;
        contextMenu = null;
        contextMenuOwner = null;
        suppressedContextMenuButton = -1;
        if (activeMenu != null) {
            activeMenu.cancel();
        }
    }

    private void clearContextMenu(GrapheneContextMenuOverlaySupport completedMenu) {
        if (contextMenu == completedMenu) {
            contextMenu = null;
            contextMenuOwner = null;
        }
    }
}
