package io.github.trethore.graphene.internal.resource;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class GrapheneMimeTypes {
  public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  private static final Map<String, String> MIME_TYPES =
      Map.ofEntries(
          Map.entry("html", "text/html"),
          Map.entry("htm", "text/html"),
          Map.entry("js", "text/javascript"),
          Map.entry("mjs", "text/javascript"),
          Map.entry("css", "text/css"),
          Map.entry("json", "application/json"),
          Map.entry("png", "image/png"),
          Map.entry("jpg", "image/jpeg"),
          Map.entry("jpeg", "image/jpeg"),
          Map.entry("gif", "image/gif"),
          Map.entry("webp", "image/webp"),
          Map.entry("ico", "image/x-icon"),
          Map.entry("svg", "image/svg+xml"),
          Map.entry("woff", "font/woff"),
          Map.entry("woff2", "font/woff2"),
          Map.entry("ttf", "font/ttf"),
          Map.entry("otf", "font/otf"),
          Map.entry("wasm", "application/wasm"));

  private GrapheneMimeTypes() {}

  public static String resolve(String path) {
    String normalizedPath = Objects.requireNonNull(path, "path").toLowerCase(Locale.ROOT);
    int extensionSeparator = normalizedPath.lastIndexOf('.');
    if (extensionSeparator < 0 || extensionSeparator == normalizedPath.length() - 1) {
      return DEFAULT_MIME_TYPE;
    }

    String extension = normalizedPath.substring(extensionSeparator + 1);
    return MIME_TYPES.getOrDefault(extension, DEFAULT_MIME_TYPE);
  }
}
