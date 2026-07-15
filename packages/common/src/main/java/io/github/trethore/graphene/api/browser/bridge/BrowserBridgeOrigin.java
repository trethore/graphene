package io.github.trethore.graphene.api.browser.bridge;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** A normalized, exact document origin used by {@link BrowserBridgePolicy}. */
public record BrowserBridgeOrigin(String scheme, String host, int port) {
  private static final int NO_PORT = -1;

  public BrowserBridgeOrigin {
    scheme = normalizeScheme(scheme);
    host = normalizeHost(host);
    if (host.isBlank() && !"classpath".equals(scheme)) {
      throw new IllegalArgumentException("host must not be blank");
    }
    port = effectivePort(scheme, port);
    if (port < NO_PORT || port > 65_535) {
      throw new IllegalArgumentException("port must be between -1 and 65535");
    }
  }

  /**
   * Extracts a normalized origin from a hierarchical URL. Opaque document URLs, file URLs, and
   * malformed URLs do not have an exposable bridge origin.
   */
  public static Optional<BrowserBridgeOrigin> fromUrl(String url) {
    try {
      URI uri = URI.create(Objects.requireNonNull(url, "url"));
      String rawScheme = uri.getScheme();
      if (rawScheme == null || rawScheme.isBlank()) {
        return Optional.empty();
      }
      String scheme = normalizeScheme(rawScheme);
      if (uri.isOpaque() || uri.getUserInfo() != null || isOpaqueDocumentScheme(scheme)) {
        return Optional.empty();
      }

      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        if (!"classpath".equals(scheme)) {
          return Optional.empty();
        }
        host = "";
      }

      return Optional.of(new BrowserBridgeOrigin(scheme, host, uri.getPort()));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  private static int effectivePort(String scheme, int port) {
    if (port >= 0) {
      return port;
    }
    return switch (scheme) {
      case "http" -> 80;
      case "https" -> 443;
      default -> NO_PORT;
    };
  }

  private static boolean isOpaqueDocumentScheme(String scheme) {
    return switch (scheme) {
      case "about", "blob", "data", "file", "javascript" -> true;
      default -> false;
    };
  }

  private static String normalizeScheme(String scheme) {
    String normalizedScheme = Objects.requireNonNull(scheme, "scheme").trim();
    if (normalizedScheme.isBlank()) {
      throw new IllegalArgumentException("scheme must not be blank");
    }
    return normalizedScheme.toLowerCase(Locale.ROOT);
  }

  private static String normalizeHost(String host) {
    return Objects.requireNonNull(host, "host").trim().toLowerCase(Locale.ROOT);
  }
}
