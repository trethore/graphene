package io.github.trethore.graphene.api.browser;

import java.util.Objects;

/** A frame load that has started. */
public record BrowserLoadStarted(String url, boolean mainFrame, BrowserLoadTransition transition) {
  public BrowserLoadStarted {
    url = Objects.requireNonNullElse(url, "");
    transition = Objects.requireNonNullElse(transition, BrowserLoadTransition.UNKNOWN);
  }
}
