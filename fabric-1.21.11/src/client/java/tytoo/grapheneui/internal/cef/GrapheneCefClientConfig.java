package tytoo.grapheneui.internal.cef;

import org.cef.CefClient;
import org.cef.browser.CefMessageRouter;
import tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime;
import tytoo.grapheneui.internal.cef.alert.GrapheneFolderUploadDialogManager;
import tytoo.grapheneui.internal.cef.alert.GrapheneJsDialogManager;
import tytoo.grapheneui.internal.core.GrapheneMainThreadExecutor;
import tytoo.grapheneui.internal.event.GrapheneLoadEventBus;
import tytoo.grapheneui.internal.event.GrapheneTitleEventBus;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.Objects;

final class GrapheneCefClientConfig {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneCefClientConfig.class);

    private GrapheneCefClientConfig() {
    }

    static void configure(
            CefClient cefClient,
            GrapheneLoadEventBus loadEventBus,
            GrapheneTitleEventBus titleEventBus,
            GrapheneBridgeRuntime bridgeRuntime,
            GrapheneCefBrowserShutdownTracker shutdownTracker,
            GrapheneMainThreadExecutor mainThreadExecutor
    ) {
        CefClient validatedClient = Objects.requireNonNull(cefClient, "cefClient");
        GrapheneLoadEventBus validatedLoadEventBus = Objects.requireNonNull(loadEventBus, "loadEventBus");
        GrapheneTitleEventBus validatedTitleEventBus = Objects.requireNonNull(titleEventBus, "titleEventBus");
        GrapheneBridgeRuntime validatedBridgeRuntime = Objects.requireNonNull(bridgeRuntime, "bridgeRuntime");
        GrapheneCefBrowserShutdownTracker validatedShutdownTracker = Objects.requireNonNull(
                shutdownTracker,
                "shutdownTracker"
        );
        GrapheneMainThreadExecutor validatedMainThreadExecutor = Objects.requireNonNull(
                mainThreadExecutor,
                "mainThreadExecutor"
        );
        GrapheneJsDialogManager jsDialogManager = new GrapheneJsDialogManager();
        GrapheneFolderUploadDialogManager folderUploadDialogManager = new GrapheneFolderUploadDialogManager();
        GrapheneCefDownloadHandler downloadHandler = new GrapheneCefDownloadHandler();
        GrapheneCefLifeSpanHandler lifeSpanHandler = new GrapheneCefLifeSpanHandler(
                validatedShutdownTracker,
                validatedMainThreadExecutor
        );
        GrapheneCefRequestHandler requestHandler = new GrapheneCefRequestHandler(validatedMainThreadExecutor);

        validatedClient.addLoadHandler(new GrapheneCefLoadHandler(
                validatedLoadEventBus,
                validatedBridgeRuntime,
                validatedMainThreadExecutor
        ));
        validatedClient.addDisplayHandler(new GrapheneCefDisplayHandler(validatedTitleEventBus, validatedMainThreadExecutor));
        validatedClient.addContextMenuHandler(new GrapheneCefContextMenuHandler());
        validatedClient.addJSDialogHandler(new GrapheneCefJsDialogHandler(jsDialogManager));
        validatedClient.addDialogHandler(new GrapheneCefFileDialogHandler(folderUploadDialogManager));
        validatedClient.addDownloadHandler(downloadHandler);
        validatedClient.addLifeSpanHandler(lifeSpanHandler);
        validatedClient.addRequestHandler(requestHandler);

        CefMessageRouter messageRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig());
        messageRouter.addHandler(new GrapheneCefMessageRouterHandler(validatedBridgeRuntime), true);
        validatedClient.addMessageRouter(messageRouter);

        DEBUG_LOGGER.debug("Configured CEF client handlers and message router for Graphene bridge runtime");
    }
}
