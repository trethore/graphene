package tytoo.grapheneui.client.cef;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;
import tytoo.grapheneui.client.browser.GrapheneBrowser;
import tytoo.grapheneui.client.event.GrapheneLoadEventBus;

public final class GrapheneCefClientConfig {
    private GrapheneCefClientConfig() {
    }

    public static void configure(CefClient cefClient, GrapheneLoadEventBus loadEventBus) {
        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                loadEventBus.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
            }

            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                loadEventBus.onLoadStart(browser, frame, transitionType);
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                loadEventBus.onLoadEnd(browser, frame, httpStatusCode);
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
    }
}
