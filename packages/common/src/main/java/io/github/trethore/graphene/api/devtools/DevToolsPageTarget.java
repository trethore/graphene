package io.github.trethore.graphene.api.devtools;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

/** Metadata for a remotely inspectable browser page target. */
public record DevToolsPageTarget(String id, String title, String url, URI inspectorUri)
    implements Serializable {
  public DevToolsPageTarget {
    id = requireNonBlank(id, "id");
    Objects.requireNonNull(title, "title");
    url = requireNonBlank(url, "url");
    Objects.requireNonNull(inspectorUri, "inspectorUri");
    if (!inspectorUri.isAbsolute()) {
      throw new IllegalArgumentException("inspectorUri must be absolute");
    }
  }

  private static String requireNonBlank(String value, String name) {
    String validatedValue = Objects.requireNonNull(value, name).trim();
    if (validatedValue.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return validatedValue;
  }
}
