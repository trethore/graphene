package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.bridge.GrapheneBridgeRuntime;
import io.github.trethore.graphene.internal.event.GrapheneLoadEventBus;
import io.github.trethore.graphene.internal.platform.GrapheneFileDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneJsDialogPresenter;
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
      GrapheneTaskExecutor mainThreadExecutor,
      GrapheneFileDialogPresenter fileDialogPresenter,
      GrapheneJsDialogPresenter jsDialogPresenter) {
    CefClient validatedClient = Objects.requireNonNull(client, "client");
    validatedClient.addLoadHandler(
        new GrapheneCefLoadHandler(eventBus, bridgeRuntime, mainThreadExecutor));
    validatedClient.addContextMenuHandler(new GrapheneCefContextMenuHandler());
    validatedClient.addLifeSpanHandler(new GrapheneCefLifeSpanHandler(mainThreadExecutor));
    validatedClient.addRequestHandler(new GrapheneCefRequestHandler(mainThreadExecutor));
    validatedClient.addDialogHandler(
        new GrapheneCefFileDialogHandler(fileDialogPresenter, mainThreadExecutor));
    validatedClient.addJSDialogHandler(
        new GrapheneCefJsDialogHandler(jsDialogPresenter, mainThreadExecutor));
    CefMessageRouter router =
        CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig());
    router.addHandler(new GrapheneCefMessageRouterHandler(bridgeRuntime), true);
    validatedClient.addMessageRouter(router);
  }
}
