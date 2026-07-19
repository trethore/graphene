package io.github.trethore.graphene.internal.url;

import io.github.trethore.graphene.api.url.AssetId;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

abstract class AbstractGrapheneSchemedAssetUrls extends AbstractGrapheneAssetUrls {
  private final String scheme;

  protected AbstractGrapheneSchemedAssetUrls(String scheme, String defaultNamespace) {
    super(defaultNamespace);
    this.scheme = Objects.requireNonNull(scheme, "scheme");
  }

  protected final String normalizeAssetResourcePath(String url) {
    URI uri = parseExpectedScheme(url);
    if (uri == null) {
      return "";
    }

    try {
      return normalizeAssetResourcePath(uri);
    } catch (IllegalArgumentException exception) {
      return "";
    }
  }

  private URI parseExpectedScheme(String url) {
    try {
      URI uri = URI.create(Objects.requireNonNull(url, "url"));
      return scheme.equalsIgnoreCase(uri.getScheme()) ? uri : null;
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private String normalizeAssetResourcePath(URI uri) {
    String decodedPath = decode(uri.getRawPath());
    String host = uri.getHost();

    if (host == null || host.isBlank()) {
      return normalizeHostlessPath(decodedPath);
    }

    if (ASSET_HOST.equalsIgnoreCase(host)) {
      return normalizeAssetsPath(decodedPath);
    }

    return resourcePath(
        AssetId.normalizeNamespace(host), normalizeDecodedResourcePath(decodedPath));
  }

  private String normalizeHostlessPath(String path) {
    String normalizedPath = stripLeadingSlashes(path);
    if (!normalizedPath.startsWith(ASSET_HOST + PATH_DELIMITER)) {
      return "";
    }

    return normalizeAssetsPath(normalizedPath.substring(ASSET_HOST.length()));
  }

  private String normalizeAssetsPath(String path) {
    String normalizedPath = stripLeadingSlashes(path);
    int namespaceSeparator = normalizedPath.indexOf(PATH_DELIMITER);
    if (namespaceSeparator <= 0 || namespaceSeparator == normalizedPath.length() - 1) {
      return "";
    }

    String namespace = AssetId.normalizeNamespace(normalizedPath.substring(0, namespaceSeparator));
    String assetPath =
        normalizeDecodedResourcePath(normalizedPath.substring(namespaceSeparator + 1));
    return resourcePath(namespace, assetPath);
  }

  private String normalizeDecodedResourcePath(String path) {
    String normalizedPath = stripLeadingSlashes(path);
    if (normalizedPath.isBlank() || normalizedPath.indexOf('\\') >= 0) {
      throw new IllegalArgumentException("Invalid resource path");
    }

    for (String segment : normalizedPath.split("/", -1)) {
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        throw new IllegalArgumentException("Invalid resource path segment");
      }

      for (int index = 0; index < segment.length(); index++) {
        if (Character.isISOControl(segment.charAt(index))) {
          throw new IllegalArgumentException("Invalid resource path character");
        }
      }
    }

    return normalizedPath;
  }

  private String resourcePath(String namespace, String path) {
    return ASSET_HOST + PATH_DELIMITER + namespace + PATH_DELIMITER + path;
  }

  private String decode(String value) {
    return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private String stripLeadingSlashes(String value) {
    String result = value;
    while (result.startsWith(PATH_DELIMITER)) {
      result = result.substring(1);
    }
    return result;
  }
}
