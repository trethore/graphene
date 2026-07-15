package io.github.trethore.graphene.internal.runtime;

import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import java.util.OptionalInt;

public interface GrapheneBrowserRuntime {
  void initialize(GrapheneGlobalConfig config);

  void shutdown();

  OptionalInt remoteDebuggingPort();

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
      public BrowserSession createSession(
          String url, BrowserOptions options, int width, int height, String grapheneHttpBaseUrl) {
        throw new IllegalStateException("Graphene browser runtime is not installed");
      }
    };
  }
}
