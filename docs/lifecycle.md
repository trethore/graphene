# Lifecycle

Understanding lifecycle rules is key to avoiding leaks and stale bridge state.

## Runtime Lifecycle

- Call `GrapheneCore.init()` once during client startup.
- Repeated calls are ignored with a warning.
- Graphene registers shutdown hooks and disposes CEF on client stop/process shutdown.

## Surface And Widget Lifecycle

- `GrapheneWebViewWidget` owns a `BrowserSurface`.
- Widget construction registers the surface to the widget owner.
- `close()` detaches bridge, closes browser, and unregisters ownership.

## Screen Auto-Close Behavior

Graphene injects `ScreenMixin` with auto-close enabled by default.

When a screen closes:

1. tracked `GrapheneWebViewWidget`s are closed
2. owned surfaces are closed from `GrapheneCore.surfaces()`
3. internal widget tracking list is cleared

This behavior prevents browser/surface leaks in normal UI flows.

## Opting Out Of Auto-Close (Advanced)

If you need to persist a surface across screen transitions:

```java
import tytoo.grapheneui.screen.GrapheneScreens;

GrapheneScreens.setWebViewAutoCloseEnabled(screen, false);
```

When opting out, you must manually close widgets/surfaces.

## Bridge Lifecycle

- On page load start: bridge readiness resets, pending Java requests fail.
- On page load end: bridge bootstrap script is injected.
- On JS `ready`: queued outbound messages flush and `onReady` listeners fire.
- On detach/close: bridge handlers, queue, and pending requests are cleared.

## Subscription Lifecycle

Subscriptions are explicit resources:

- `GrapheneBridgeSubscription`
- `BrowserSurface.Subscription` (for load listeners)

Always unsubscribe when no longer needed, or scope with try-with-resources.

```java
try (GrapheneBridgeSubscription sub = bridge.onEvent("my-mod:event", (_, payload) -> {})) {
    // use subscription
}
```

---
Next: [Debugging](debugging.md)
