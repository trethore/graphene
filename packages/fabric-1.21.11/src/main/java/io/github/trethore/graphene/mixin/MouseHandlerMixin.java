package io.github.trethore.graphene.mixin;

import io.github.trethore.graphene.fabric.internal.input.GrapheneMouseHandlerHook;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
@SuppressWarnings({"java:S100", "java:S116"})
public abstract class MouseHandlerMixin {
  @Redirect(
      method = "onButton",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
  private boolean grapheneui$forwardMouseRelease(Screen screen, MouseButtonEvent event) {
    return GrapheneMouseHandlerHook.handleMouseRelease(screen, event);
  }
}
