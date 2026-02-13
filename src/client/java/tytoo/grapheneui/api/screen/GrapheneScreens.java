package tytoo.grapheneui.api.screen;

import net.minecraft.client.gui.screens.Screen;
import tytoo.grapheneui.internal.screen.GrapheneScreenBridge;

import java.util.Objects;

public final class GrapheneScreens {
    private GrapheneScreens() {
    }

    public static boolean isWebViewAutoCloseEnabled(Screen screen) {
        return requireBridge(screen).graphene$isWebViewAutoCloseEnabled();
    }

    public static void setWebViewAutoCloseEnabled(Screen screen, boolean autoClose) {
        requireBridge(screen).graphene$setWebViewAutoCloseEnabled(autoClose);
    }

    private static GrapheneScreenBridge requireBridge(Screen screen) {
        Objects.requireNonNull(screen, "screen");
        if (screen instanceof GrapheneScreenBridge screenBridge) {
            return screenBridge;
        }

        throw new IllegalStateException("Screen does not implement GrapheneScreenBridge: " + screen.getClass().getName());
    }
}
