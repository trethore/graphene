package io.github.trethore.graphene.internal.runtime;

import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import java.util.OptionalInt;

public interface GrapheneBrowserRuntime {
  void initialize(GrapheneGlobalConfig config);

  void shutdown();

  OptionalInt remoteDebuggingPort();

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
    };
  }
}
