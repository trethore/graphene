package io.github.trethore.graphene.fabric.api.surface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class BrowserSurfaceInputAdapterTest {
  @Test
  void recognizesPlainPasteShortcut() {
    int shortcutModifier =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
            ? GLFW.GLFW_MOD_SUPER
            : GLFW.GLFW_MOD_CONTROL;

    assertTrue(BrowserSurfaceInputAdapter.isPasteShortcut(GLFW.GLFW_KEY_V, shortcutModifier));
    assertFalse(BrowserSurfaceInputAdapter.isPasteShortcut(GLFW.GLFW_KEY_V, 0));
    assertFalse(
        BrowserSurfaceInputAdapter.isPasteShortcut(
            GLFW.GLFW_KEY_V, shortcutModifier | GLFW.GLFW_MOD_SHIFT));
    assertFalse(BrowserSurfaceInputAdapter.isPasteShortcut(GLFW.GLFW_KEY_C, shortcutModifier));
  }
}
