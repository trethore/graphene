package io.github.trethore.graphene.mixin;

import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenBridge;
import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
  @Unique private final GrapheneScreenState grapheneui$state = new GrapheneScreenState();

  @Override
  public GrapheneScreenState graphene$state() {
    return grapheneui$state;
  }

  @Inject(method = "onClose", at = @At("HEAD"))
  private void grapheneui$onClose(CallbackInfo callbackInfo) {
    grapheneui$state.closeWebViews();
  }

  @Inject(method = "resize", at = @At("HEAD"))
  private void grapheneui$onResize(int width, int height, CallbackInfo callbackInfo) {
    grapheneui$state.resizeWebViews();
  }

  @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
  private void grapheneui$renderContextMenu(
      GuiGraphicsExtractor graphics,
      int mouseX,
      int mouseY,
      float partialTick,
      CallbackInfo callbackInfo) {
    graphene$renderContextMenu(graphics, mouseX, mouseY);
  }
}
