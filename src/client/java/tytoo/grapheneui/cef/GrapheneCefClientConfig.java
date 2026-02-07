package tytoo.grapheneui.cef;

import org.cef.CefClient;
import org.cef.browser.CefMessageRouter;
import tytoo.grapheneui.bridge.internal.GrapheneBridgeRuntime;
import tytoo.grapheneui.cef.alert.GrapheneJsDialogManager;
import tytoo.grapheneui.event.GrapheneLoadEventBus;

import java.util.Objects;

public final class GrapheneCefClientConfig {
    private static final GrapheneJsDialogManager JS_DIALOG_MANAGER = new GrapheneJsDialogManager();

    private GrapheneCefClientConfig() {
    }

    public static void configure(CefClient cefClient, GrapheneLoadEventBus loadEventBus, GrapheneBridgeRuntime bridgeRuntime) {
        CefClient validatedClient = Objects.requireNonNull(cefClient, "cefClient");
        GrapheneLoadEventBus validatedLoadEventBus = Objects.requireNonNull(loadEventBus, "loadEventBus");
        GrapheneBridgeRuntime validatedBridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");

        validatedClient.addLoadHandler(new GrapheneCefLoadHandler(validatedLoadEventBus, validatedBridgeRuntime));
        validatedClient.addDisplayHandler(new GrapheneCefDisplayHandler());
        validatedClient.addJSDialogHandler(new GrapheneCefJsDialogHandler(JS_DIALOG_MANAGER));

        CefMessageRouter messageRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig());
        messageRouter.addHandler(new GrapheneCefMessageRouterHandler(validatedBridgeRuntime), true);
        validatedClient.addMessageRouter(messageRouter);
    }
}
