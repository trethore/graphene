package io.github.trethore.graphene.api.browser.bridge;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether the Java/JavaScript bridge is exposed to a main-frame document. Implementations
 * must be thread-safe and must not block. Exceptions and {@code null} decisions deny exposure.
 */
@FunctionalInterface
public interface BrowserBridgePolicy {
  Decision decide(Request request);

  /** Allows only Graphene-owned app, classpath, and built-in HTTP documents. */
  static BrowserBridgePolicy defaultPolicy() {
    return grapheneOwnedDocuments();
  }

  static BrowserBridgePolicy grapheneOwnedDocuments() {
    return request -> request.source().grapheneOwned() ? Decision.ALLOW : Decision.DENY;
  }

  static BrowserBridgePolicy disabled() {
    return request -> Decision.DENY;
  }

  /** Allows the exact normalized origin of the URL requested when the browser was created. */
  static BrowserBridgePolicy initialOrigin() {
    return request ->
        request.origin().isPresent() && request.origin().equals(request.initialOrigin())
            ? Decision.ALLOW
            : Decision.DENY;
  }

  /** Allows an exact set of normalized origins. Wildcards are not supported. */
  static BrowserBridgePolicy allowOrigins(Set<BrowserBridgeOrigin> origins) {
    Set<BrowserBridgeOrigin> allowedOrigins =
        Set.copyOf(Objects.requireNonNull(origins, "origins"));
    return request ->
        request.origin().filter(allowedOrigins::contains).isPresent()
            ? Decision.ALLOW
            : Decision.DENY;
  }

  static BrowserBridgePolicy allowOrigins(BrowserBridgeOrigin... origins) {
    Objects.requireNonNull(origins, "origins");
    return allowOrigins(Set.copyOf(Arrays.asList(origins)));
  }

  /** Decision to expose or withhold the bridge from a document. */
  enum Decision {
    ALLOW,
    DENY
  }

  /** Document information used to decide whether the bridge is exposed. */
  record Request(
      String documentUrl,
      BrowserBridgeDocumentSource source,
      Optional<BrowserBridgeOrigin> origin,
      Optional<BrowserBridgeOrigin> initialOrigin) {
    public Request {
      Objects.requireNonNull(documentUrl, "documentUrl");
      Objects.requireNonNull(source, "source");
      Objects.requireNonNull(origin, "origin");
      Objects.requireNonNull(initialOrigin, "initialOrigin");
    }
  }
}
