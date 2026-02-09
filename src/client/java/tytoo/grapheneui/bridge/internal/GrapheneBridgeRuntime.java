package tytoo.grapheneui.bridge.internal;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefQueryCallback;
import tytoo.grapheneui.bridge.GrapheneBridge;
import tytoo.grapheneui.browser.GrapheneBrowser;

import java.util.*;

public final class GrapheneBridgeRuntime {
    private static final String BROWSER_NAME = "browser";

    private final Object lock = new Object();
    private final Map<CefBrowser, GrapheneBridgeEndpoint> endpointsByBrowser = new IdentityHashMap<>();

    public GrapheneBridge attach(GrapheneBrowser browser) {
        Objects.requireNonNull(browser, BROWSER_NAME);

        GrapheneBridgeEndpoint previousEndpoint;
        GrapheneBridgeEndpoint newEndpoint = new GrapheneBridgeEndpoint(browser);
        synchronized (lock) {
            previousEndpoint = endpointsByBrowser.put(browser, newEndpoint);
        }

        if (previousEndpoint != null) {
            previousEndpoint.close();
        }

        return newEndpoint;
    }

    public void detach(CefBrowser browser) {
        Objects.requireNonNull(browser, BROWSER_NAME);

        GrapheneBridgeEndpoint endpoint;
        synchronized (lock) {
            endpoint = endpointsByBrowser.remove(browser);
        }

        if (endpoint != null) {
            endpoint.close();
        }
    }

    public void onLoadStart(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint != null) {
            endpoint.onPageLoadStart();
        }
    }

    public void onLoadEnd(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint != null) {
            endpoint.onPageLoadEnd();
        }
    }

    public boolean onQuery(CefBrowser browser, String request, CefQueryCallback callback) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint == null) {
            return false;
        }

        return endpoint.handleQuery(request, callback);
    }

    public void onQueryCanceled(CefBrowser browser, long queryId) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint != null) {
            endpoint.onQueryCanceled(queryId);
        }
    }

    public void shutdown() {
        List<GrapheneBridgeEndpoint> endpoints;
        synchronized (lock) {
            endpoints = new ArrayList<>(endpointsByBrowser.values());
            endpointsByBrowser.clear();
        }

        for (GrapheneBridgeEndpoint endpoint : endpoints) {
            endpoint.close();
        }
    }

    private GrapheneBridgeEndpoint endpoint(CefBrowser browser) {
        Objects.requireNonNull(browser, BROWSER_NAME);

        synchronized (lock) {
            return endpointsByBrowser.get(browser);
        }
    }
}
