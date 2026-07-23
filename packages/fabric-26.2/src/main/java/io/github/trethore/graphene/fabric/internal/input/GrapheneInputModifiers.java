package io.github.trethore.graphene.fabric.internal.input;

import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import java.util.EnumSet;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

public final class GrapheneInputModifiers {
  private GrapheneInputModifiers() {}

  public static Set<BrowserModifier> fromGlfw(int modifiers) {
    EnumSet<BrowserModifier> result = EnumSet.noneOf(BrowserModifier.class);
    add(result, modifiers, GLFW.GLFW_MOD_SHIFT, BrowserModifier.SHIFT);
    add(result, modifiers, GLFW.GLFW_MOD_CONTROL, BrowserModifier.CONTROL);
    add(result, modifiers, GLFW.GLFW_MOD_ALT, BrowserModifier.ALT);
    add(result, modifiers, GLFW.GLFW_MOD_SUPER, BrowserModifier.META);
    add(result, modifiers, GLFW.GLFW_MOD_CAPS_LOCK, BrowserModifier.CAPS_LOCK);
    add(result, modifiers, GLFW.GLFW_MOD_NUM_LOCK, BrowserModifier.NUM_LOCK);
    return Set.copyOf(result);
  }

  private static void add(
      Set<BrowserModifier> result, int modifiers, int flag, BrowserModifier modifier) {
    if ((modifiers & flag) != 0) {
      result.add(modifier);
    }
  }
}
