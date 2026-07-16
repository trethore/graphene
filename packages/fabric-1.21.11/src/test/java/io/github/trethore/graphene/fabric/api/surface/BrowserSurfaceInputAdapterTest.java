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
    int shortcutModifier = shortcutModifier();

    assertTrue(BrowserSurfaceInputAdapter.isPasteShortcut(GLFW.GLFW_KEY_V, shortcutModifier));
    assertFalse(BrowserSurfaceInputAdapter.isPasteShortcut(GLFW.GLFW_KEY_V, 0));
    assertFalse(
        BrowserSurfaceInputAdapter.isPasteShortcut(
            GLFW.GLFW_KEY_V, shortcutModifier | GLFW.GLFW_MOD_SHIFT));
    assertFalse(BrowserSurfaceInputAdapter.isPasteShortcut(GLFW.GLFW_KEY_C, shortcutModifier));
  }

  @Test
  void recognizesClipboardWriteShortcuts() {
    int shortcutModifier = shortcutModifier();

    assertTrue(
        BrowserSurfaceInputAdapter.isClipboardWriteShortcut(GLFW.GLFW_KEY_C, shortcutModifier));
    assertTrue(
        BrowserSurfaceInputAdapter.isClipboardWriteShortcut(GLFW.GLFW_KEY_X, shortcutModifier));
    assertFalse(
        BrowserSurfaceInputAdapter.isClipboardWriteShortcut(GLFW.GLFW_KEY_V, shortcutModifier));
    assertFalse(BrowserSurfaceInputAdapter.isClipboardWriteShortcut(GLFW.GLFW_KEY_C, 0));
    assertFalse(
        BrowserSurfaceInputAdapter.isClipboardWriteShortcut(
            GLFW.GLFW_KEY_C, shortcutModifier | GLFW.GLFW_MOD_SHIFT));
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

  @Test
  void preservesUnicodeTextWithoutAllocatingAnUnchangedReplacement() {
    String text = "\uD83D\uDE00e\u0301";

    assertSame(text, BrowserSurfaceInputAdapter.normalizeText(text));
  }

  @Test
  void normalizesSupportedControlsAndFiltersUnsupportedControls() {
    assertEquals("a\b\rb", BrowserSurfaceInputAdapter.normalizeText("a\u007F\n\uF700\u0001b"));
    assertNull(BrowserSurfaceInputAdapter.normalizeText("\uF700\u0001"));
  }

  private static int shortcutModifier() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
        ? GLFW.GLFW_MOD_SUPER
        : GLFW.GLFW_MOD_CONTROL;
  }
}
