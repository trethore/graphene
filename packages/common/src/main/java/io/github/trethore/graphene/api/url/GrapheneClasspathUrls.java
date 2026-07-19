package io.github.trethore.graphene.api.url;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Creates and interprets {@code classpath:///assets/<namespace>/<path>} asset URLs. */
public final class GrapheneClasspathUrls {
  public static final String SCHEME = "classpath";

  private static final GrapheneClasspathUrlsSupport SUPPORT =
      new GrapheneClasspathUrlsSupport("grapheneui");

  private GrapheneClasspathUrls() {}

  public static String url(String namespace, String path) {
    return SUPPORT.url(namespace, path);
  }

  public static String url(AssetId assetId) {
    return SUPPORT.url(assetId);
  }

  public static GrapheneAssetUrls assets() {
    return SUPPORT;
  }

  public static GrapheneAssetUrls assets(String namespace) {
    return new GrapheneClasspathUrlsSupport(namespace);
  }

  /** Returns the normalized classpath resource path, or an empty string for an invalid URL. */
  public static String normalizeResourcePath(String url) {
    return SUPPORT.normalizeResourcePath(url);
  }

  private static final class GrapheneClasspathUrlsSupport implements GrapheneAssetUrls {
    private static final String ROOT_PREFIX = SCHEME + ":///assets/";

    private final String defaultNamespace;

    private GrapheneClasspathUrlsSupport(String defaultNamespace) {
      this.defaultNamespace = AssetId.normalizeNamespace(defaultNamespace);
    }

    private String normalizeResourcePath(String url) {
      try {
        URI uri = URI.create(Objects.requireNonNull(url, "url"));
        if (!SCHEME.equalsIgnoreCase(uri.getScheme())) {
          return "";
        }

        String decodedPath =
            URLDecoder.decode(
                uri.getRawPath() == null ? "" : uri.getRawPath(), StandardCharsets.UTF_8);
        while (decodedPath.startsWith("/")) {
          decodedPath = decodedPath.substring(1);
        }

        if (!decodedPath.startsWith("assets/")) {
          return "";
        }

        validateResourcePath(decodedPath);
        return decodedPath;
      } catch (IllegalArgumentException exception) {
        return "";
      }
    }

    private void validateResourcePath(String resourcePath) {
      String[] segments = resourcePath.split("/", -1);
      if (segments.length < 3 || !segments[0].equals("assets")) {
        throw new IllegalArgumentException("Invalid asset resource path");
      }

      AssetId.normalizeNamespace(segments[1]);
      for (int index = 2; index < segments.length; index++) {
        String segment = segments[index];
        if (segment.isEmpty()
            || segment.equals(".")
            || segment.equals("..")
            || segment.indexOf('\\') >= 0) {
          throw new IllegalArgumentException("Invalid asset resource path segment");
        }
      }
    }

    @Override
    public String url(String path) {
      return url(defaultNamespace, path);
    }

    @Override
    public String url(String namespace, String path) {
      return url(AssetId.of(namespace, path));
    }

    @Override
    public String url(AssetId assetId) {
      AssetId validatedAssetId = Objects.requireNonNull(assetId, "assetId");
      return ROOT_PREFIX + validatedAssetId.namespace() + "/" + validatedAssetId.path();
    }
  }
}
