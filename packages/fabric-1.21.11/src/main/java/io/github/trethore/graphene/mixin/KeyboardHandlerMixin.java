package io.github.trethore.graphene.mixin;

import io.github.trethore.graphene.fabric.api.widget.GrapheneWebViewWidget;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"java:S100"})
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
  @Shadow @Final private Minecraft minecraft;

  @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
  private void graphene$routeGlobalKeyToBrowser(
      long windowHandle, int action, KeyEvent event, CallbackInfo callbackInfo) {
    if (action != 1 || windowHandle != minecraft.getWindow().handle()) {
      return;
    }

    Screen screen = minecraft.screen;
    if (screen == null || !(screen.getFocused() instanceof GrapheneWebViewWidget webView)) {
      return;
    }

    if (!minecraft.options.keyFullscreen.matches(event)
        && !minecraft.options.keyScreenshot.matches(event)) {
      return;
    }

    screen.afterKeyboardAction();
    webView.keyPressed(event);
    callbackInfo.cancel();
  }
}
