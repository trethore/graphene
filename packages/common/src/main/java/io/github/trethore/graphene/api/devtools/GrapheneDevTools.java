package io.github.trethore.graphene.api.devtools;

import io.github.trethore.graphene.api.browser.BrowserSession;
import java.util.List;
import java.util.concurrent.CompletionStage;

/** Read-only access to Graphene's remote DevTools discovery endpoint. */
public interface GrapheneDevTools {
  /** Returns whether remote debugging is currently enabled and available for discovery. */
  boolean isEnabled();

  /**
   * Discovers the current remotely inspectable page targets.
   *
   * <p>The returned stage completes exceptionally with {@link DevToolsDisabledException} if remote
   * debugging is disabled, {@link DevToolsRuntimeUnavailableException} if the Graphene runtime is
   * not running, or {@link DevToolsDiscoveryException} if the discovery endpoint cannot be queried
   * or parsed.
   */
  CompletionStage<List<DevToolsPageTarget>> pageTargets();

  /**
   * Discovers the remote page target associated with a Graphene browser session.
   *
   * <p>The returned stage completes exceptionally with {@link DevToolsDisabledException} if remote
   * debugging is disabled, {@link DevToolsRuntimeUnavailableException} if the Graphene runtime is
   * not running, {@link DevToolsTargetNotFoundException} if no page target matches the session,
   * {@link DevToolsTargetAmbiguousException} if multiple page targets match the session, or {@link
   * DevToolsDiscoveryException} if the discovery endpoint cannot be queried or parsed.
   */
  CompletionStage<DevToolsPageTarget> targetFor(BrowserSession session);
}
