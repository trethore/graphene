package tytoo.grapheneui.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.browser.GrapheneWebViewWidget;
import tytoo.grapheneui.screen.GrapheneScreenBridge;

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
    public List<GrapheneWebViewWidget> grapheneui$getWebViewWidgets() {
        return grapheneui$webViewWidgets;
    }

    @Override
    public void grapheneui$addWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneui$webViewWidgets.add(webViewWidget);
    }

    @Override
    public void grapheneui$removeWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneui$webViewWidgets.remove(webViewWidget);
    }

    @Override
    public boolean grapheneui$isAutoCloseWebViews() {
        return grapheneui$autoCloseWebViews;
    }

    @Override
    public void grapheneui$setAutoCloseWebViews(boolean autoClose) {
        grapheneui$autoCloseWebViews = autoClose;
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

        GrapheneCore.surfaces().closeOwner(this);

        grapheneui$webViewWidgets.clear();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void grapheneui$onResize(int width, int height, CallbackInfo callbackInfo) {
        for (GrapheneWebViewWidget webViewWidget : grapheneui$webViewWidgets) {
            webViewWidget.handleScreenResize();
        }
    }
}
