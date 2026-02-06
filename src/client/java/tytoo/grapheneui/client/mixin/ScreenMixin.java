package tytoo.grapheneui.client.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tytoo.grapheneui.client.browser.GrapheneWebViewWidget;
import tytoo.grapheneui.client.screen.GrapheneScreenBridge;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler implements Renderable, GrapheneScreenBridge {
    @Unique
    private final List<GrapheneWebViewWidget> grapheneUi$webViewWidgets = new ArrayList<>();

    @Unique
    private boolean grapheneUi$autoCloseWebViews = true;

    @Override
    public List<GrapheneWebViewWidget> grapheneUi$getWebViewWidgets() {
        return grapheneUi$webViewWidgets;
    }

    @Override
    public void grapheneUi$addWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneUi$webViewWidgets.add(webViewWidget);
    }

    @Override
    public void grapheneUi$removeWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneUi$webViewWidgets.remove(webViewWidget);
    }

    @Override
    public boolean grapheneUi$isAutoCloseWebViews() {
        return grapheneUi$autoCloseWebViews;
    }

    @Override
    public void grapheneUi$setAutoCloseWebViews(boolean autoClose) {
        grapheneUi$autoCloseWebViews = autoClose;
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void grapheneUi$onClose(CallbackInfo callbackInfo) {
        if (!grapheneUi$autoCloseWebViews) {
            return;
        }

        List<GrapheneWebViewWidget> widgetsToClose = new ArrayList<>(grapheneUi$webViewWidgets);
        for (GrapheneWebViewWidget webViewWidget : widgetsToClose) {
            webViewWidget.close();
        }

        grapheneUi$webViewWidgets.clear();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void grapheneUi$onResize(int width, int height, CallbackInfo callbackInfo) {
        for (GrapheneWebViewWidget webViewWidget : grapheneUi$webViewWidgets) {
            webViewWidget.handleScreenResize();
        }
    }
}
