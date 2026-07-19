package io.github.trethore.graphene.internal.runtime;

import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.devtools.DevToolsDisabledException;
import io.github.trethore.graphene.api.devtools.DevToolsPageTarget;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface GrapheneBrowserRuntime {
  void initialize(GrapheneGlobalConfig config);

  void shutdown();

  OptionalInt remoteDebuggingPort();

  CompletionStage<List<DevToolsPageTarget>> devToolsPageTargets();

  CompletionStage<DevToolsPageTarget> devToolsTargetFor(BrowserSession session);

  BrowserSession createSession(
      String url, BrowserOptions options, int width, int height, String grapheneHttpBaseUrl);

  static GrapheneBrowserRuntime disabled() {
    return new GrapheneBrowserRuntime() {
      @Override
      public void initialize(GrapheneGlobalConfig config) {
        // A disabled browser runtime intentionally allocates no native resources.
      }

      @Override
      public void shutdown() {
        // A disabled browser runtime has no native resources to release.
      }

      @Override
      public OptionalInt remoteDebuggingPort() {
        return OptionalInt.empty();
      }

      @Override
      public CompletionStage<List<DevToolsPageTarget>> devToolsPageTargets() {
        return CompletableFuture.failedFuture(new DevToolsDisabledException());
      }

      @Override
      public CompletionStage<DevToolsPageTarget> devToolsTargetFor(BrowserSession session) {
        return CompletableFuture.failedFuture(new DevToolsDisabledException());
      }

      @Override
      public BrowserSession createSession(
          String url, BrowserOptions options, int width, int height, String grapheneHttpBaseUrl) {
        throw new IllegalStateException("Graphene browser runtime is not installed");
      }
    };
  }
}
