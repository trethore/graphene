package io.github.trethore.graphene.fabric.api.screen;

import io.github.trethore.graphene.fabric.internal.screen.GrapheneScreenBridge;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;

@SuppressWarnings("unused")
public final class GrapheneScreens {
  private GrapheneScreens() {}

  public static boolean isWebViewAutoCloseEnabled(Screen screen) {
    return requireBridge(screen).graphene$isWebViewAutoCloseEnabled();
  }

  public static void setWebViewAutoCloseEnabled(Screen screen, boolean autoClose) {
    requireBridge(screen).graphene$setWebViewAutoCloseEnabled(autoClose);
  }

  private static GrapheneScreenBridge requireBridge(Screen screen) {
    Screen validatedScreen = Objects.requireNonNull(screen, "screen");
    if (validatedScreen instanceof GrapheneScreenBridge bridge) {
      return bridge;
    }
    throw new IllegalStateException(
        "Screen does not implement GrapheneScreenBridge: " + screen.getClass().getName());
  }
}
