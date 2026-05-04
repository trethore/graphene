package tytoo.grapheneui.internal.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
    public List<GrapheneWebViewWidget> graphene$webViewWidgets() {
        return grapheneui$webViewWidgets;
    }

    @Override
    public void graphene$addWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneui$webViewWidgets.add(webViewWidget);
    }

    @Override
    public void graphene$removeWebViewWidget(GrapheneWebViewWidget webViewWidget) {
        grapheneui$webViewWidgets.remove(webViewWidget);
    }

    @Override
    public boolean graphene$isWebViewAutoCloseEnabled() {
        return grapheneui$autoCloseWebViews;
    }

    @Override
    public void graphene$setWebViewAutoCloseEnabled(boolean autoClose) {
        grapheneui$autoCloseWebViews = autoClose;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (grapheneui$isCursorCapturedByFocusedWebView()) {
            return true;
        }

        return getChildAt(mouseButtonEvent.x(), mouseButtonEvent.y())
                .map(child -> grapheneui$mouseClickedChild(child, mouseButtonEvent, isDoubleClick))
                .orElse(false);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent mouseButtonEvent) {
        GuiEventListener focused = this.getFocused();
        if (grapheneui$isCursorCapturedByFocusedWebView()) {
            if (mouseButtonEvent.button() == 0) {
                this.setDragging(false);
            }

            return true;
        }

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

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (grapheneui$isCursorCapturedByFocusedWebView()) {
            return true;
        }

        GuiEventListener focused = this.getFocused();
        return focused != null && this.isDragging() && mouseButtonEvent.button() == 0
                && focused.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (grapheneui$isCursorCapturedByFocusedWebView()) {
            return true;
        }

        return getChildAt(mouseX, mouseY)
                .filter(child -> child.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
                .isPresent();
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int grapheneui$renderMouseX(int mouseX) {
        return grapheneui$isCursorCapturedByFocusedWebView() ? Integer.MIN_VALUE : mouseX;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int grapheneui$renderMouseY(int mouseY) {
        return grapheneui$isCursorCapturedByFocusedWebView() ? Integer.MIN_VALUE : mouseY;
    }

    @Unique
    private boolean grapheneui$mouseClickedChild(GuiEventListener child, MouseButtonEvent mouseButtonEvent, boolean isDoubleClick) {
        if (child.mouseClicked(mouseButtonEvent, isDoubleClick) && child.shouldTakeFocusAfterInteraction()) {
            this.setFocused(child);
            if (mouseButtonEvent.button() == 0) {
                this.setDragging(true);
            }
        }

        return true;
    }

    @Unique
    private boolean grapheneui$isCursorCapturedByFocusedWebView() {
        GuiEventListener focused = this.getFocused();
        return focused instanceof GrapheneWebViewWidget webViewWidget && webViewWidget.isCursorCaptured();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void grapheneui$keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!keyEvent.isEscape()) {
            return;
        }

        GuiEventListener focused = this.getFocused();
        if (!(focused instanceof GrapheneWebViewWidget webViewWidget)) {
            return;
        }

        if (webViewWidget.consumeScreenEscape(keyEvent)) {
            callbackInfo.setReturnValue(true);
        }
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
