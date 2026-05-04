package tytoo.grapheneui.internal.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tytoo.grapheneui.api.widget.GrapheneWebViewWidget;

@Mixin(MouseHandler.class)
@SuppressWarnings({"java:S100", "java:S116"}) // Yes sonar this is a mixin.
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(
            method = "handleAccumulatedMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;afterMouseMove()V",
                    shift = At.Shift.AFTER
            )
    )
    private void grapheneui$handleAccumulatedMovement(CallbackInfo callbackInfo) {
        Screen screen = minecraft.screen;
        if (screen == null) {
            return;
        }

        GuiEventListener focused = screen.getFocused();
        if (!(focused instanceof GrapheneWebViewWidget webViewWidget) || !webViewWidget.isCursorCaptured()) {
            return;
        }

        Window window = minecraft.getWindow();
        double scaledDeltaX = MouseHandler.getScaledXPos(window, accumulatedDX);
        double scaledDeltaY = MouseHandler.getScaledYPos(window, accumulatedDY);
        webViewWidget.cursorCaptureMoved(scaledDeltaX, scaledDeltaY);
    }
}
