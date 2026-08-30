package io.github.trethore.graphene.mixin;

import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
@SuppressWarnings("java:S100")
public abstract class ScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void grapheneui$renderContextMenu(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        GrapheneScreenBridge bridge = (GrapheneScreenBridge) this;
        bridge.graphene$renderContextMenu((GrapheneGuiGraphics) graphics, mouseX, mouseY);
    }
}
