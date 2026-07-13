package io.github.trethore.graphene.internal.platform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class GrapheneClipboardTest {
  @Test
  void transfersTextHtmlAndPng() throws Exception {
    GrapheneClipboard clipboard = new GrapheneClipboard(new Clipboard("graphene-test"));
    byte[] png = createPng();

    clipboard.write(new GrapheneClipboardContent("plain", "<b>rich</b>", png));
    GrapheneClipboardContent result = clipboard.read();

    assertEquals("plain", result.text());
    assertEquals("<b>rich</b>", result.html());
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(result.png()));
    assertEquals(2, image.getWidth());
    assertEquals(2, image.getHeight());
  }

  @Test
  void clipboardContentDefensivelyCopiesPngBytes() {
    byte[] png = {1, 2, 3};
    GrapheneClipboardContent content = new GrapheneClipboardContent(null, null, png);

    png[0] = 9;
    byte[] returnedPng = content.png();
    returnedPng[1] = 9;

    assertArrayEquals(new byte[] {1, 2, 3}, content.png());
    assertFalse(content.isEmpty());
  }

  @Test
  void toleratesUnavailableSystemClipboard() {
    GrapheneClipboard clipboard = new GrapheneClipboard(null);

    clipboard.write(new GrapheneClipboardContent("plain", null, null));

    assertFalse(clipboard.isAvailable());
    assertTrue(clipboard.read().isEmpty());
  }

  private static byte[] createPng() throws Exception {
    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, 0xFFFF0000);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }
}
