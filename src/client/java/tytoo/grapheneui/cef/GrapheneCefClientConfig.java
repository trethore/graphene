package tytoo.grapheneui.cef;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.network.CefRequest;
import tytoo.grapheneui.bridge.internal.GrapheneBridgeRuntime;
import tytoo.grapheneui.browser.GrapheneBrowser;
import tytoo.grapheneui.event.GrapheneLoadEventBus;

public final class GrapheneCefClientConfig {
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
