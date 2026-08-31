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
GrapheneWebViewWidget -> BrowserView -> BrowserSession
```

- Closing a widget closes its input adapter and owned view.
- Closing a view closes its GPU texture and owned browser session.
- GUI and world surfaces borrow their view and do not close it.
- Closing a session directly invalidates further state-changing operations.
- Closing a surface input adapter removes adapter subscriptions but does not close its view.
- Closing a `GrapheneSubscription` removes only its listener or handler.

Constructing a `GrapheneWebViewWidget` with an existing `BrowserView` or `BrowserGuiSurface` transfers ownership of the
view to the widget. Closing the widget then closes that view. Do not pass a view that must remain available to another
longer-lived projection.

Close the highest-level object you own instead of closing every nested object independently.

## Reuse a widget during resize

Minecraft can call `init()` again when a screen changes size. Keep the widget in a field, update its position and size,
then add it back to the renderable-widget list. This preserves its browser session and avoids unnecessary recreation.

`GrapheneWebViewWidget.setSize(...)` updates the surface's logical size. It also updates browser resolution while the
surface uses automatic resolution. Custom integrations can call `handleScreenResize()` to resynchronize automatic
resolution after a screen-size change or GUI-scale update.

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
