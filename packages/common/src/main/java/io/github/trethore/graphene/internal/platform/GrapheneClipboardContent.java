package io.github.trethore.graphene.internal.platform;

import java.util.Arrays;

public record GrapheneClipboardContent(String text, String html, byte[] png) {
  public GrapheneClipboardContent {
    png = png == null ? new byte[0] : Arrays.copyOf(png, png.length);
  }

  @Override
  public byte[] png() {
    return Arrays.copyOf(png, png.length);
  }

  public boolean isEmpty() {
    return (text == null || text.isEmpty()) && (html == null || html.isEmpty()) && png.length == 0;
  }
}
