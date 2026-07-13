package io.github.trethore.graphene.mixin;

import io.github.trethore.graphene.fabric.internal.input.GrapheneKeyboardHandlerHook;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"java:S100"})
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
  @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
  private void graphene$routeGlobalKeyToBrowser(
      long windowHandle, int action, KeyEvent event, CallbackInfo callbackInfo) {
    if (GrapheneKeyboardHandlerHook.handleKeyPress(windowHandle, action, event)) {
      callbackInfo.cancel();
    }
  }
}
