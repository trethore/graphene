package tytoo.grapheneui.internal.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;
import tytoo.grapheneui.internal.screen.GrapheneScreenBridge;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
@SuppressWarnings({"java:S100", "java:S116"}) // Yes sonar this is a mixin.
public abstract class ScreenMixin extends AbstractContainerEventHandler implements Renderable, GrapheneScreenBridge {
    @Unique
    private final List<GrapheneWebViewWidget> grapheneui$webViewWidgets = new ArrayList<>();

    @Unique
    private boolean grapheneui$autoCloseWebViews = true;

    @Override
    public List<GrapheneWebViewWidget> grapheneWebViewWidgets() {
        return grapheneui$webViewWidgets;
    }

    @Override
    public void addGrapheneWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneui$webViewWidgets.add(webViewWidget);
    }

    @Override
    public void removeGrapheneWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneui$webViewWidgets.remove(webViewWidget);
    }

    @Override
    public boolean isGrapheneWebViewAutoCloseEnabled() {
        return grapheneui$autoCloseWebViews;
    }

    @Override
    public void setGrapheneWebViewAutoCloseEnabled(boolean autoClose) {
        grapheneui$autoCloseWebViews = autoClose;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        GuiEventListener focused = this.getFocused();
        if (mouseButtonEvent.button() == 0 && this.isDragging()) {
            this.setDragging(false);
            if (focused != null && focused != this) {
                return focused.mouseReleased(mouseButtonEvent);
            }

            return false;
        }

        if (focused instanceof GrapheneWebViewWidget) {
            return focused.mouseReleased(mouseButtonEvent);
        }

        return false;
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void grapheneui$onClose(CallbackInfo callbackInfo) {
        if (!grapheneui$autoCloseWebViews) {
            return;
        }

        List<GrapheneWebViewWidget> widgetsToClose = new ArrayList<>(grapheneui$webViewWidgets);
        for (GrapheneWebViewWidget webViewWidget : widgetsToClose) {
            webViewWidget.close();
        }

        GrapheneCore.closeOwnedSurfaces(this);

        grapheneui$webViewWidgets.clear();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void grapheneui$onResize(int width, int height, CallbackInfo callbackInfo) {
        for (GrapheneWebViewWidget webViewWidget : grapheneui$webViewWidgets) {
            webViewWidget.handleScreenResize();
        }
    }
}
