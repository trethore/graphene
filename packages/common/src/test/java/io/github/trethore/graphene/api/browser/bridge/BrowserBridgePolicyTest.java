package io.github.trethore.graphene.api.browser.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

final class BrowserBridgePolicyTest {
  @Test
  void normalizesSupportedOriginsAndDefaultPorts() {
    assertEquals(
        new BrowserBridgeOrigin("https", "example.com", 443),
        new BrowserBridgeOrigin("HTTPS", "EXAMPLE.COM", -1));
    assertEquals(
        Optional.of(new BrowserBridgeOrigin("https", "example.com", 443)),
        BrowserBridgeOrigin.fromUrl("HTTPS://EXAMPLE.COM/path"));
    assertEquals(
        Optional.of(new BrowserBridgeOrigin("http", "example.com", 8080)),
        BrowserBridgeOrigin.fromUrl("http://example.com:8080/path"));
    assertEquals(
        Optional.of(new BrowserBridgeOrigin("app", "assets", -1)),
        BrowserBridgeOrigin.fromUrl("app://assets/test/index.html"));
    assertEquals(
        Optional.of(new BrowserBridgeOrigin("classpath", "", -1)),
        BrowserBridgeOrigin.fromUrl("classpath:///assets/test/index.html"));
  }

  @Test
  void rejectsOpaqueAndFileOrigins() {
    assertTrue(BrowserBridgeOrigin.fromUrl("about:blank").isEmpty());
    assertTrue(BrowserBridgeOrigin.fromUrl("data:text/html,test").isEmpty());
    assertTrue(BrowserBridgeOrigin.fromUrl("file:///tmp/index.html").isEmpty());
  }

  @Test
  void initialOriginDoesNotFollowCrossOriginNavigation() {
    BrowserBridgeOrigin initialOrigin =
        BrowserBridgeOrigin.fromUrl("https://example.com/index.html").orElseThrow();
    BrowserBridgePolicy policy = BrowserBridgePolicy.initialOrigin();

    assertEquals(
        BrowserBridgePolicy.Decision.ALLOW,
        policy.decide(request("https://example.com/next", initialOrigin)));
    assertEquals(
        BrowserBridgePolicy.Decision.DENY,
        policy.decide(request("https://other.example/next", initialOrigin)));
  }

  @Test
  void originAllowlistUsesExactNormalizedOrigins() {
    BrowserBridgeOrigin allowedOrigin =
        BrowserBridgeOrigin.fromUrl("https://example.com").orElseThrow();
    BrowserBridgePolicy policy = BrowserBridgePolicy.allowOrigins(allowedOrigin);

    assertEquals(
        BrowserBridgePolicy.Decision.ALLOW,
        policy.decide(request("https://example.com/path", allowedOrigin)));
    assertEquals(
        BrowserBridgePolicy.Decision.DENY,
        policy.decide(request("https://example.com:8443/path", allowedOrigin)));
  }

  private static BrowserBridgePolicy.Request request(
      String documentUrl, BrowserBridgeOrigin initialOrigin) {
    return new BrowserBridgePolicy.Request(
        documentUrl,
        BrowserBridgeDocumentSource.OTHER,
        BrowserBridgeOrigin.fromUrl(documentUrl),
        Optional.of(initialOrigin));
  }
}
