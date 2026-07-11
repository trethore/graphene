package io.github.trethore.graphene.mixin;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenBridge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
@SuppressWarnings({"java:S100", "java:S116"})
public abstract class ScreenMixin extends AbstractContainerEventHandler
    implements Renderable, GrapheneScreenBridge {
  @Unique private final List<GrapheneWebViewWidget> grapheneui$webViewWidgets = new ArrayList<>();
  @Unique private boolean grapheneui$autoCloseWebViews = true;

  @Override
  public List<GrapheneWebViewWidget> graphene$webViewWidgets() {
    return grapheneui$webViewWidgets;
  }

  @Override
  public void graphene$addWebViewWidget(GrapheneWebViewWidget widget) {
    grapheneui$webViewWidgets.add(widget);
  }

  @Override
  public void graphene$removeWebViewWidget(GrapheneWebViewWidget widget) {
    grapheneui$webViewWidgets.remove(widget);
  }

  @Override
  public boolean graphene$isWebViewAutoCloseEnabled() {
    return grapheneui$autoCloseWebViews;
  }

  @Override
  public void graphene$setWebViewAutoCloseEnabled(boolean autoClose) {
    grapheneui$autoCloseWebViews = autoClose;
  }

  @Inject(method = "onClose", at = @At("HEAD"))
  private void grapheneui$onClose(CallbackInfo callbackInfo) {
    if (!grapheneui$autoCloseWebViews) {
      return;
    }
    List<GrapheneWebViewWidget> widgetsToClose = new ArrayList<>(grapheneui$webViewWidgets);
    widgetsToClose.forEach(GrapheneWebViewWidget::close);
    grapheneui$webViewWidgets.clear();
  }

  @Inject(method = "resize", at = @At("HEAD"))
  private void grapheneui$onResize(int width, int height, CallbackInfo callbackInfo) {
    grapheneui$webViewWidgets.forEach(GrapheneWebViewWidget::handleScreenResize);
  }
}
