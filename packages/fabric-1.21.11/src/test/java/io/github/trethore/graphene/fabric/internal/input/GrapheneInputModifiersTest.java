package io.github.trethore.graphene.fabric.internal.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class GrapheneInputModifiersTest {
  @Test
  void mapsGlfwModifierFlags() {
    int flags =
        GLFW.GLFW_MOD_SHIFT
            | GLFW.GLFW_MOD_CONTROL
            | GLFW.GLFW_MOD_ALT
            | GLFW.GLFW_MOD_SUPER
            | GLFW.GLFW_MOD_CAPS_LOCK
            | GLFW.GLFW_MOD_NUM_LOCK;

    assertEquals(Set.of(BrowserModifier.values()), GrapheneInputModifiers.fromGlfw(flags));
    assertTrue(GrapheneInputModifiers.fromGlfw(0).isEmpty());
  }
}
