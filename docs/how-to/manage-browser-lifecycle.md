# Manage Browser Lifecycle

Graphene has a process-wide runtime and consumer-owned browser objects. Correct registration and cleanup keep those
lifetimes aligned with Fabric and Minecraft screens.

## Register during client initialization

Call `Graphene.register(...)` from your client initializer. Registration closes when the platform starts Graphene's
shared runtime.

```java
@Override
public void onInitializeClient() {
  context = Graphene.register(ExampleModClient.class);
}
```

Registering the same mod again with the same configuration returns the existing context. Registering it with different
configuration fails.

## Observe runtime initialization

The runtime moves through `NEW`, `STARTING`, `RUNNING`, `STOPPING`, `STOPPED`, or `FAILED`.

```java
GrapheneRuntime runtime = context.runtime();

runtime.initialization().whenComplete((ignored, failure) -> {
  if (failure != null) {
    LOGGER.error("Graphene failed to initialize", failure);
  }
});
```

Creating a browser while the runtime is not `RUNNING` throws `BrowserRuntimeUnavailableException`. Normal screens opened
after client startup can create widgets directly. Early integration code should wait for `initialization()`.

## Let screens close widgets automatically

`GrapheneWebViewWidget` registers itself with its containing screen. Closing the screen closes registered web-view
widgets by default.

Disable automatic closure only when the same screen instance intentionally retains a browser session across temporary
closes:

```java
GrapheneScreens.setWebViewAutoCloseEnabled(this, false);
```

When auto-close is disabled, the screen owns explicit cleanup:

```java
if (webView != null) {
  webView.close();
  webView = null;
}
```

## Respect ownership

Ownership flows downward:

```text
GrapheneWebViewWidget -> BrowserSurface -> BrowserSession
```

- Closing a widget closes its input adapter and owned surface.
- Closing a surface closes its renderer and owned browser session.
- Closing a session directly invalidates further state-changing operations.
- Closing `BrowserSurfaceInputAdapter` removes adapter subscriptions but does not close its surface.
- Closing a `GrapheneSubscription` removes only its listener or handler.

Close the highest-level object you own instead of closing every nested object independently.

## Reuse a widget during resize

Minecraft can call `init()` again when a screen changes size. Keep the widget in a field, update its position and size,
then add it back to the renderable-widget list. This preserves its browser session and avoids unnecessary recreation.

`GrapheneWebViewWidget.setSize(...)` updates the surface resolution. Custom integrations can call `handleScreenResize()`
to resynchronize resolution after a screen-size change.

## Persistent bridge handlers

Java bridge subscriptions belong to the code that registered them. Remove them when replacing handlers or explicitly
closing a persistent browser:

```java
subscriptions.forEach(GrapheneSubscription::unsubscribe);
subscriptions.clear();
```

Page-side JavaScript listeners and handlers disappear when the document navigates. Register them during each page load.

## Next steps

- [Understand the browser layers](../explanation/browser-layers.md).
- [Troubleshoot leaked or unavailable browsers](troubleshoot.md).
