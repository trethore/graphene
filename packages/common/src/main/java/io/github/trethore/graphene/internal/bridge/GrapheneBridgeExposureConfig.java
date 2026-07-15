package io.github.trethore.graphene.internal.bridge;

import io.github.trethore.graphene.api.browser.bridge.BrowserBridgePolicy;
import java.util.Objects;

public record GrapheneBridgeExposureConfig(
    BrowserBridgePolicy policy, String initialUrl, String grapheneHttpBaseUrl) {
  public GrapheneBridgeExposureConfig {
    Objects.requireNonNull(policy, "policy");
    Objects.requireNonNull(initialUrl, "initialUrl");
    Objects.requireNonNull(grapheneHttpBaseUrl, "grapheneHttpBaseUrl");
  }
}
