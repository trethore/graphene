package io.github.trethore.graphene.api.url;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated asset namespace and resource path. Values use lowercase resource-identifier
 * characters, and paths cannot contain empty, current-directory, or parent-directory segments.
 */
public record AssetId(String namespace, String path) {
  private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9._-]+");
  private static final Pattern PATH_PATTERN = Pattern.compile("[a-z0-9/._-]+");

  public AssetId {
    namespace = normalizeNamespace(namespace);
    path = normalizePath(path);
  }

  public static AssetId of(String namespace, String path) {
    return new AssetId(namespace, path);
  }

  public static String normalizeNamespace(String namespace) {
    String normalizedNamespace = Objects.requireNonNull(namespace, "namespace").trim();
    if (!NAMESPACE_PATTERN.matcher(normalizedNamespace).matches()) {
      throw new IllegalArgumentException(
          "namespace must contain only lowercase letters, digits, '.', '_' or '-'");
    }

    return normalizedNamespace;
  }

  public static String normalizePath(String path) {
    String normalizedPath = Objects.requireNonNull(path, "path").trim();
    while (normalizedPath.startsWith("/")) {
      normalizedPath = normalizedPath.substring(1);
    }

    if (normalizedPath.isBlank()) {
      throw new IllegalArgumentException("path must not be blank");
    }

    if (!PATH_PATTERN.matcher(normalizedPath).matches()) {
      throw new IllegalArgumentException(
          "path must contain only lowercase letters, digits, '/', '.', '_' or '-'");
    }

    String[] segments = normalizedPath.split("/", -1);
    for (String segment : segments) {
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        throw new IllegalArgumentException(
            "path must not contain empty, current-directory or parent-directory segments");
      }
    }

    return normalizedPath;
  }
}
