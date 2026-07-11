package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.bridge.GrapheneBridgeRuntime;
import io.github.trethore.graphene.internal.event.GrapheneLoadEventBus;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import org.cef.CefClient;
import org.cef.browser.CefMessageRouter;

final class GrapheneCefClientConfig {
  private GrapheneCefClientConfig() {}

  static void configure(
      CefClient client,
      GrapheneLoadEventBus eventBus,
      GrapheneBridgeRuntime bridgeRuntime,
      GrapheneTaskExecutor mainThreadExecutor) {
    CefClient validatedClient = Objects.requireNonNull(client, "client");
    validatedClient.addLoadHandler(
        new GrapheneCefLoadHandler(eventBus, bridgeRuntime, mainThreadExecutor));
    CefMessageRouter router =
        CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig());
    router.addHandler(new GrapheneCefMessageRouterHandler(bridgeRuntime), true);
    validatedClient.addMessageRouter(router);
  }
}
