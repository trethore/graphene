package io.github.trethore.graphene.internal.platform;

import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneClipboard {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneClipboard.class);

  private final Clipboard clipboard;

  public GrapheneClipboard() {
    this(systemClipboard());
  }

  GrapheneClipboard(Clipboard clipboard) {
    this.clipboard = clipboard;
  }

  public boolean isAvailable() {
    return clipboard != null;
  }

  public synchronized GrapheneClipboardContent read() {
    if (clipboard == null) {
      return new GrapheneClipboardContent(null, null, null);
    }
    try {
      Transferable contents = clipboard.getContents(null);
      if (contents == null) {
        return new GrapheneClipboardContent(null, null, null);
      }
      return new GrapheneClipboardContent(
          readString(contents, DataFlavor.stringFlavor), readHtml(contents), readPng(contents));
    } catch (IllegalStateException exception) {
      LOGGER.debug("Desktop clipboard is temporarily unavailable", exception);
      return new GrapheneClipboardContent(null, null, null);
    }
  }

  public synchronized void write(GrapheneClipboardContent content) {
    GrapheneClipboardContent validatedContent = Objects.requireNonNull(content, "content");
    if (clipboard == null || validatedContent.isEmpty()) {
      return;
    }
    try {
      Transferable contents = new ClipboardTransferable(validatedContent);
      if (contents.getTransferDataFlavors().length > 0) {
        clipboard.setContents(contents, null);
      }
    } catch (IllegalStateException | IOException exception) {
      LOGGER.warn("Failed to update the desktop clipboard", exception);
    }
  }

  private static String readHtml(Transferable contents) {
    for (DataFlavor flavor : htmlFlavors()) {
      String html = readString(contents, flavor);
      if (html != null) {
        return html;
      }
    }
    return null;
  }

  private static Clipboard systemClipboard() {
    try {
      return Toolkit.getDefaultToolkit().getSystemClipboard();
    } catch (HeadlessException exception) {
      LOGGER.warn("Rich desktop clipboard support is unavailable in headless mode");
      return null;
    }
  }

  private static String readString(Transferable contents, DataFlavor flavor) {
    if (!contents.isDataFlavorSupported(flavor)) {
      return null;
    }
    try {
      Object value = contents.getTransferData(flavor);
      return value instanceof String string ? string : null;
    } catch (UnsupportedFlavorException | IOException exception) {
      LOGGER.debug("Failed to read clipboard flavor {}", flavor.getMimeType(), exception);
      return null;
    }
  }

  private static byte[] readPng(Transferable contents) {
    if (!contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      return new byte[0];
    }
    try {
      Object value = contents.getTransferData(DataFlavor.imageFlavor);
      if (!(value instanceof Image image)) {
        return new byte[0];
      }
      BufferedImage bufferedImage = toBufferedImage(image);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      return ImageIO.write(bufferedImage, "png", output) ? output.toByteArray() : new byte[0];
    } catch (UnsupportedFlavorException | IOException | IllegalArgumentException exception) {
      LOGGER.debug("Failed to read clipboard image", exception);
      return new byte[0];
    }
  }

  private static BufferedImage toBufferedImage(Image image) {
    if (image instanceof BufferedImage bufferedImage) {
      return bufferedImage;
    }
    int width = image.getWidth(null);
    int height = image.getHeight(null);
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Clipboard image has invalid dimensions");
    }
    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = bufferedImage.createGraphics();
    try {
      graphics.drawImage(image, 0, 0, null);
    } finally {
      graphics.dispose();
    }
    return bufferedImage;
  }

  private static List<DataFlavor> htmlFlavors() {
    return List.of(
        DataFlavor.selectionHtmlFlavor, DataFlavor.fragmentHtmlFlavor, DataFlavor.allHtmlFlavor);
  }

  private static final class ClipboardTransferable implements Transferable {
    private final String text;
    private final String html;
    private final BufferedImage image;
    private final DataFlavor[] flavors;

    private ClipboardTransferable(GrapheneClipboardContent content) throws IOException {
      text = content.text();
      html = content.html();
      byte[] png = content.png();
      image = png.length == 0 ? null : ImageIO.read(new ByteArrayInputStream(png));
      ArrayList<DataFlavor> availableFlavors = new ArrayList<>();
      if (text != null) {
        availableFlavors.add(DataFlavor.stringFlavor);
      }
      if (html != null) {
        availableFlavors.addAll(htmlFlavors());
      }
      if (image != null) {
        availableFlavors.add(DataFlavor.imageFlavor);
      }
      flavors = availableFlavors.toArray(DataFlavor[]::new);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      for (DataFlavor availableFlavor : flavors) {
        if (availableFlavor.equals(flavor)) {
          return true;
        }
      }
      return false;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (DataFlavor.stringFlavor.equals(flavor) && text != null) {
        return text;
      }
      if (htmlFlavors().contains(flavor) && html != null) {
        return html;
      }
      if (DataFlavor.imageFlavor.equals(flavor) && image != null) {
        return image;
      }
      throw new UnsupportedFlavorException(flavor);
    }
  }
}
