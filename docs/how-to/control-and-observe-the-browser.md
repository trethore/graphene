# Control and Observe the Browser

`BrowserSession` is the loader-independent browser API. Obtain it from a widget's surface:

```java
BrowserSession browser = webView.surface().browser();
```

or from a custom surface:

```java
BrowserSession browser = surface.browser();
```

## Navigate and inspect state

```java
browser.navigate(context.appAssets().url("ui/settings.html"));
browser.goBack();
browser.goForward();
browser.reload();
browser.stopLoading();

String url = browser.currentUrl();
String title = browser.currentTitle();
boolean loading = browser.isLoading();
boolean canGoBack = browser.canGoBack();
```

The convenience methods on `GrapheneWebViewWidget` expose the common navigation operations.

## Run page scripts

```java
browser.executeScript("document.body.dataset.mode = 'compact';");
```

Use the [bridge](../tutorials/connect-java-and-javascript.md) for structured application communication. Reserve
`executeScript` for browser-oriented operations that do not need a stable message contract.

## Change zoom

```java
browser.setZoomLevel(1.0);
double zoomLevel = browser.zoomLevel();
browser.resetZoom();
```

The default zoom level is `0.0`; positive values magnify and negative values reduce the page.

## Find text

```java
browser.startFinding(new BrowserFindQuery("graphene", false));
browser.findNext(BrowserFindDirection.FORWARD);
browser.findNext(BrowserFindDirection.BACKWARD);
browser.stopFinding();
```

Starting a new query replaces the active search.

## Observe loads

```java
GrapheneSubscription loadSubscription =
        browser.onLoad(
                new BrowserLoadListener() {
                    @Override
                    public void onLoadCompleted(BrowserLoadCompleted event) {
                        if (event.mainFrame()) {
                            LOGGER.info("Loaded {}", event.url());
                        }
                    }

                    @Override
                    public void onLoadFailed(BrowserLoadFailed event) {
                        LOGGER.error("Failed to load {}: {}", event.url(), event.message());
                    }
                });
```

`onLoad` reports loading-state changes, starts, completions, and failures. Load callbacks are delivered on the platform
thread.

## Observe URL, title, and console messages

```java
GrapheneSubscription urlSubscription =
    browser.onUrlChanged(url -> LOGGER.debug("URL: {}", url));
GrapheneSubscription titleSubscription =
    browser.onTitleChanged(title -> LOGGER.debug("Title: {}", title));
GrapheneSubscription consoleSubscription =
    browser.onConsoleMessage(message ->
        LOGGER.info("Browser {}: {}", message.severity(), message.message()));
```

These deduplicated callbacks are delivered on the platform thread.

## Observe rendered frames

`latestFrame()` returns the most recent complete off-screen frame. `onFrame(...)` reports latest-only frame snapshots;
Graphene may coalesce intermediate frames when rendering runs faster than the platform thread consumes them.

Most consumers should let `BrowserSurface` upload and render frames instead of reading pixel buffers directly.

## Observe and cancel downloads

```java
GrapheneSubscription downloadSubscription =
    browser.onDownloadChanged(
        download -> {
          if (download.percentComplete().orElse(0) > 50 && shouldCancel(download)) {
            download.control().cancel();
          }
        });
```

Download callbacks run on the browser callback thread and must not block. `BrowserDownloadControl.cancel()` is
thread-safe.

## Clean up subscriptions

Keep each returned `GrapheneSubscription` with the component that registered it:

```java
subscriptions.forEach(GrapheneSubscription::unsubscribe);
subscriptions.clear();
```

Cleanup is idempotent. Remove listeners and handlers when replacing a page integration or closing a persistent browser.

## Next steps

- [Configure navigation and download policies](configure-browser-policies.md).
- [Manage browser lifecycle](manage-browser-lifecycle.md).
- [Look up the core Java API](../reference/core-java-api.md).
