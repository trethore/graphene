package io.github.trethore.graphene.fabric.api.surface;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.internal.platform.GrapheneClipboardContent;
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

  @Test
  void prefersNativeTextWhenRichClipboardIsStale() {
    GrapheneClipboardContent richContent =
        new GrapheneClipboardContent("old", "<b>old</b>", new byte[] {1, 2, 3});

    GrapheneClipboardContent result =
        BrowserSurfaceInputAdapter.resolveClipboardContent(richContent, "external");

    assertEquals("external", result.text());
    assertNull(result.html());
    assertArrayEquals(new byte[0], result.png());
  }

  @Test
  void preservesRichClipboardWhenNativeTextMatches() {
    GrapheneClipboardContent richContent =
        new GrapheneClipboardContent("shared", "<b>shared</b>", new byte[] {1, 2, 3});

    GrapheneClipboardContent result =
        BrowserSurfaceInputAdapter.resolveClipboardContent(richContent, "shared");

    assertSame(richContent, result);
  }
}
