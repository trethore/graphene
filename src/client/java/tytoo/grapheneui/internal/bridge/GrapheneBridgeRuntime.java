package tytoo.grapheneui.internal.bridge;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefQueryCallback;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.*;

public final class GrapheneBridgeRuntime {
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneBridgeRuntime.class);

    private static final String BROWSER_NAME = "browser";
    private static final int MIN_BROWSER_IDENTIFIER = 1;

    private final Object lock = new Object();
    private final GrapheneBridgeOptions options;
    private final Map<CefBrowser, GrapheneBridgeEndpoint> endpointsByBrowser = new IdentityHashMap<>();
    private final Map<Integer, GrapheneBridgeEndpoint> endpointsByBrowserId = new HashMap<>();

    public GrapheneBridgeRuntime() {
        this(GrapheneBridgeOptions.defaults());
    }

    public GrapheneBridgeRuntime(GrapheneBridgeOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    private static int browserIdentifier(CefBrowser browser) {
        try {
            return browser.getIdentifier();
        } catch (RuntimeException _) {
            return -1;
        }
    }

    public GrapheneBridge attach(GrapheneBrowser browser) {
        Objects.requireNonNull(browser, BROWSER_NAME);

        GrapheneBridgeEndpoint previousEndpoint;
        GrapheneBridgeEndpoint newEndpoint = new GrapheneBridgeEndpoint(browser, options);
        synchronized (lock) {
            previousEndpoint = endpointsByBrowser.put(browser, newEndpoint);
            if (previousEndpoint != null) {
                removeEndpointMappingsLocked(previousEndpoint);
            }
            cacheEndpointByIdentifierLocked(browser, newEndpoint);

            DEBUG_LOGGER.debug(
                    "Attached bridge endpoint browserId={} replaced={} trackedBrowsers={}",
                    browserIdentifier(browser),
                    previousEndpoint != null,
                    endpointsByBrowser.size()
            );
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
            endpoint = endpointLocked(browser);
            if (endpoint != null) {
                removeEndpointMappingsLocked(endpoint);
            }

            DEBUG_LOGGER.debug(
                    "Detaching bridge endpoint browserId={} found={} trackedBrowsers={}",
                    browserIdentifier(browser),
                    endpoint != null,
                    endpointsByBrowser.size()
            );
        }

        if (endpoint != null) {
            endpoint.close();
        }
    }

    public void onLoadStart(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint != null) {
            endpoint.onPageLoadStart();
            DEBUG_LOGGER.debug("Bridge onLoadStart browserId={}", browserIdentifier(browser));
        }
    }

    public void onNavigationRequested(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint != null) {
            endpoint.onNavigationRequested();
            DEBUG_LOGGER.debug("Bridge onNavigationRequested browserId={}", browserIdentifier(browser));
        }
    }

    public void onLoadEnd(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint != null) {
            endpoint.onPageLoadEnd();
            DEBUG_LOGGER.debug("Bridge onLoadEnd browserId={}", browserIdentifier(browser));
        }
    }

    public void ensureBootstrap(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint == null) {
            return;
        }

        endpoint.tryBootstrapFallback();
        DEBUG_LOGGER.debug("Bridge ensureBootstrap browserId={}", browserIdentifier(browser));
    }

    public boolean onQuery(CefBrowser browser, String request, CefQueryCallback callback) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint == null) {
            DEBUG_LOGGER.debug("Bridge query ignored because endpoint is missing browserId={}", browserIdentifier(browser));
            return false;
        }

        boolean handled = endpoint.handleQuery(request, callback);
        DEBUG_LOGGER.debugIfEnabled(logger -> {
            int requestSize = request == null ? 0 : request.length();
            logger.debug(
                    "Bridge query routed browserId={} requestSize={} handled={}",
                    browserIdentifier(browser),
                    requestSize,
                    handled
            );
        });

        return handled;
    }

    public void onQueryCanceled(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpoint(browser);
        if (endpoint != null) {
            endpoint.onQueryCanceled();
            DEBUG_LOGGER.debug("Bridge query canceled browserId={}", browserIdentifier(browser));
        }
    }

    public void shutdown() {
        List<GrapheneBridgeEndpoint> endpoints;
        synchronized (lock) {
            endpoints = new ArrayList<>(endpointsByBrowser.values());
            endpointsByBrowser.clear();
            endpointsByBrowserId.clear();

            DEBUG_LOGGER.debug("Shutting down bridge runtime endpointCount={}", endpoints.size());
        }

        for (GrapheneBridgeEndpoint endpoint : endpoints) {
            endpoint.close();
        }
    }

    private GrapheneBridgeEndpoint endpoint(CefBrowser browser) {
        Objects.requireNonNull(browser, BROWSER_NAME);

        synchronized (lock) {
            return endpointLocked(browser);
        }
    }

    private GrapheneBridgeEndpoint endpointLocked(CefBrowser browser) {
        GrapheneBridgeEndpoint endpoint = endpointsByBrowser.get(browser);
        if (endpoint != null) {
            cacheEndpointByIdentifierLocked(browser, endpoint);
            return endpoint;
        }

        int browserIdentifier = browserIdentifier(browser);
        if (browserIdentifier < MIN_BROWSER_IDENTIFIER) {
            return null;
        }

        endpoint = endpointsByBrowserId.get(browserIdentifier);
        if (endpoint != null) {
            return endpoint;
        }

        for (Map.Entry<CefBrowser, GrapheneBridgeEndpoint> entry : endpointsByBrowser.entrySet()) {
            if (browserIdentifier(entry.getKey()) != browserIdentifier) {
                continue;
            }

            GrapheneBridgeEndpoint matchedEndpoint = entry.getValue();
            endpointsByBrowserId.put(browserIdentifier, matchedEndpoint);
            return matchedEndpoint;
        }

        return null;
    }

    private void cacheEndpointByIdentifierLocked(CefBrowser browser, GrapheneBridgeEndpoint endpoint) {
        int browserIdentifier = browserIdentifier(browser);
        if (browserIdentifier >= MIN_BROWSER_IDENTIFIER) {
            endpointsByBrowserId.put(browserIdentifier, endpoint);
        }
    }

    private void removeEndpointMappingsLocked(GrapheneBridgeEndpoint endpoint) {
        endpointsByBrowser.values().removeIf(existingEndpoint -> existingEndpoint == endpoint);
        endpointsByBrowserId.values().removeIf(existingEndpoint -> existingEndpoint == endpoint);
    }
}
