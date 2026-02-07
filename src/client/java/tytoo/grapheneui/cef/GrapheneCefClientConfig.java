package tytoo.grapheneui.cef;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefJSDialogCallback;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.bridge.internal.GrapheneBridgeRuntime;
import tytoo.grapheneui.browser.GrapheneBrowser;
import tytoo.grapheneui.cef.alert.GrapheneJsDialogManager;
import tytoo.grapheneui.event.GrapheneLoadEventBus;

public final class GrapheneCefClientConfig {
    private static final GrapheneJsDialogManager JS_DIALOG_MANAGER = new GrapheneJsDialogManager();

    private GrapheneCefClientConfig() {
    }

    public static void configure(CefClient cefClient, GrapheneLoadEventBus loadEventBus, GrapheneBridgeRuntime bridgeRuntime) {
        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                loadEventBus.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
            }

            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                loadEventBus.onLoadStart(browser, frame, transitionType);
                bridgeRuntime.onLoadStart(browser);
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                loadEventBus.onLoadEnd(browser, frame, httpStatusCode);
                bridgeRuntime.onLoadEnd(browser);
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                loadEventBus.onLoadError(browser, frame, errorCode, errorText, failedUrl);
            }
        });

        cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                if (browser instanceof GrapheneBrowser grapheneBrowser) {
                    grapheneBrowser.onTitleChange(title);
                }
            }
        });

        cefClient.addJSDialogHandler(new CefJSDialogHandlerAdapter() {
            @Override
            public boolean onJSDialog(
                    CefBrowser browser,
                    String originUrl,
                    CefJSDialogHandler.JSDialogType dialogType,
                    String messageText,
                    String defaultPromptText,
                    CefJSDialogCallback callback,
                    BoolRef suppressMessage
            ) {
                if (callback == null) {
                    suppressMessage.set(true);
                    GrapheneCore.LOGGER.warn(
                            "Suppressed JavaScript dialog without callback (type={}, origin={})",
                            dialogType,
                            originUrl
                    );
                    return false;
                }

                JS_DIALOG_MANAGER.enqueueDialog(browser, originUrl, dialogType, messageText, defaultPromptText, callback);
                return true;
            }
        });

        CefMessageRouter messageRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig());
        messageRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(
                    CefBrowser browser,
                    CefFrame frame,
                    long queryId,
                    String request,
                    boolean persistent,
                    CefQueryCallback callback
            ) {
                return bridgeRuntime.onQuery(browser, request, callback);
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
                bridgeRuntime.onQueryCanceled(browser, queryId);
            }
        }, true);
        cefClient.addMessageRouter(messageRouter);
    }
}
