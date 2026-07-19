package io.github.trethore.graphene.internal.bridge;

import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneBridgeRuntime {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgeRuntime.class);

  private static final String BROWSER_NAME = "browser";
  private static final int MIN_BROWSER_IDENTIFIER = 1;

  private final Object lock = new Object();
  private final GrapheneTaskExecutor taskExecutor;
  private final Map<BridgeBrowser, GrapheneBridgeEndpoint> endpointsByBrowser =
      new IdentityHashMap<>();
  private final Map<Integer, GrapheneBridgeEndpoint> endpointsByBrowserId = new HashMap<>();

  public GrapheneBridgeRuntime(GrapheneTaskExecutor taskExecutor) {
    this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
  }

  private static int browserIdentifier(BridgeBrowser browser) {
    try {
      return browser.identifier();
    } catch (RuntimeException ignored) {
      return -1;
    }
  }

  public GrapheneBridge attach(BridgeBrowser browser, GrapheneBridgeExposureConfig exposureConfig) {
    Objects.requireNonNull(browser, BROWSER_NAME);

    GrapheneBridgeEndpoint previousEndpoint;
    GrapheneBridgeEndpoint newEndpoint =
        new GrapheneBridgeEndpoint(browser, taskExecutor, exposureConfig);
    synchronized (lock) {
      previousEndpoint = endpointsByBrowser.put(browser, newEndpoint);
      if (previousEndpoint != null) {
        removeEndpointMappingsLocked(previousEndpoint);
      }
      cacheEndpointByIdentifierLocked(browser, newEndpoint);

      LOGGER.debug(
          "Attached bridge endpoint browserId={} replaced={} trackedBrowsers={}",
          browserIdentifier(browser),
          previousEndpoint != null,
          endpointsByBrowser.size());
    }

    if (previousEndpoint != null) {
      previousEndpoint.close();
    }

    return newEndpoint;
  }

  public void detach(BridgeBrowser browser) {
    Objects.requireNonNull(browser, BROWSER_NAME);

    GrapheneBridgeEndpoint endpoint;
    synchronized (lock) {
      endpoint = endpointLocked(browser);
      if (endpoint != null) {
        removeEndpointMappingsLocked(endpoint);
      }

      LOGGER.debug(
          "Detaching bridge endpoint browserId={} found={} trackedBrowsers={}",
          browserIdentifier(browser),
          endpoint != null,
          endpointsByBrowser.size());
    }

    if (endpoint != null) {
      endpoint.close();
    }
  }

  public void onLoadStart(BridgeBrowser browser) {
    GrapheneBridgeEndpoint endpoint = endpoint(browser);
    if (endpoint != null) {
      endpoint.onNavigationRequested();
      LOGGER.debug("Bridge onLoadStart browserId={}", browserIdentifier(browser));
    }
  }

  public void onNavigationRequested(BridgeBrowser browser) {
    GrapheneBridgeEndpoint endpoint = endpoint(browser);
    if (endpoint != null) {
      endpoint.onNavigationRequested();
      LOGGER.debug("Bridge onNavigationRequested browserId={}", browserIdentifier(browser));
    }
  }

  public void onLoadEnd(BridgeBrowser browser, String documentUrl) {
    GrapheneBridgeEndpoint endpoint = endpoint(browser);
    if (endpoint != null) {
      endpoint.onPageLoadEnd(documentUrl);
      LOGGER.debug("Bridge onLoadEnd browserId={}", browserIdentifier(browser));
    }
  }

  public void ensureBootstrap(BridgeBrowser browser) {
    GrapheneBridgeEndpoint endpoint = endpoint(browser);
    if (endpoint == null) {
      return;
    }

    endpoint.tryBootstrapFallback();
    LOGGER.debug("Bridge ensureBootstrap browserId={}", browserIdentifier(browser));
  }

  public boolean onQuery(
      BridgeBrowser browser, BridgeFrame frame, String request, BridgeQueryCallback callback) {
    GrapheneBridgeEndpoint endpoint = endpoint(browser);
    if (endpoint == null) {
      LOGGER.debug(
          "Bridge query ignored because endpoint is missing browserId={}",
          browserIdentifier(browser));
      return false;
    }

    boolean handled = endpoint.handleQuery(frame, request, callback);
    if (LOGGER.isDebugEnabled()) {
      int requestSize = request == null ? 0 : request.length();
      LOGGER.debug(
          "Bridge query routed browserId={} requestSize={} handled={}",
          browserIdentifier(browser),
          requestSize,
          handled);
    }

    return handled;
  }

  public void shutdown() {
    List<GrapheneBridgeEndpoint> endpoints;
    synchronized (lock) {
      endpoints = new ArrayList<>(endpointsByBrowser.values());
      endpointsByBrowser.clear();
      endpointsByBrowserId.clear();

      LOGGER.debug("Shutting down bridge runtime endpointCount={}", endpoints.size());
    }

    for (GrapheneBridgeEndpoint endpoint : endpoints) {
      endpoint.close();
    }
  }

  private GrapheneBridgeEndpoint endpoint(BridgeBrowser browser) {
    Objects.requireNonNull(browser, BROWSER_NAME);

    synchronized (lock) {
      return endpointLocked(browser);
    }
  }

  private GrapheneBridgeEndpoint endpointLocked(BridgeBrowser browser) {
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

    for (Map.Entry<BridgeBrowser, GrapheneBridgeEndpoint> entry : endpointsByBrowser.entrySet()) {
      if (browserIdentifier(entry.getKey()) != browserIdentifier) {
        continue;
      }

      GrapheneBridgeEndpoint matchedEndpoint = entry.getValue();
      endpointsByBrowserId.put(browserIdentifier, matchedEndpoint);
      return matchedEndpoint;
    }

    return null;
  }

  private void cacheEndpointByIdentifierLocked(
      BridgeBrowser browser, GrapheneBridgeEndpoint endpoint) {
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
