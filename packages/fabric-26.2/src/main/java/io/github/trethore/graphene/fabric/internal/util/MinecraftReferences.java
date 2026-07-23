package io.github.trethore.graphene.fabric.internal.util;

import com.mojang.blaze3d.platform.Window;
import java.net.URI;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;

@SuppressWarnings("resource")
public final class MinecraftReferences {
  private MinecraftReferences() {}

  public static long windowHandle() {
    return window().handle();
  }

  public static int windowWidth() {
    return window().getWidth();
  }

  public static int windowHeight() {
    return window().getHeight();
  }

  public static double guiScale() {
    return window().getGuiScale();
  }

  public static KeyboardHandler keyboardHandler() {
    return client().keyboardHandler;
  }

  public static Options options() {
    return client().options;
  }

  public static boolean hasControlDown() {
    return client().hasControlDown();
  }

  public static Screen screen() {
    return client().gui.screen();
  }

  public static Font font() {
    return client().font;
  }

  public static Overlay overlay() {
    return client().gui.overlay();
  }

  public static void setOverlay(Overlay overlay) {
    client().gui.setOverlay(overlay);
  }

  public static void execute(Runnable action) {
    client().execute(action);
  }

  public static void openUri(String url) {
    Util.getPlatform().openUri(URI.create(url));
  }

  private static Minecraft client() {
    return Minecraft.getInstance();
  }

  private static Window window() {
    return client().getWindow();
  }
}
