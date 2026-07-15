package io.github.trethore.graphene.internal.bridge;

import java.util.Objects;

public record BridgeFrame(String url, boolean mainFrame) {
  public BridgeFrame {
    Objects.requireNonNull(url, "url");
  }
}
