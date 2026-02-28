# Lifecycle

Understanding Graphene lifecycle rules prevents stale bridge state and browser leaks.

## Runtime Lifecycle

- Register every consumer with `GrapheneCore.register(...)` before first Graphene usage.
- Re-registering the same `modId` is allowed only when config is identical.
- Different config for the same `modId` throws `IllegalStateException`.
- Runtime initializes automatically after client startup when at least one consumer is registered.
- Runtime can also initialize lazily on first `GrapheneCore.runtime()` or first surface creation.

If no consumer is registered, first Graphene usage fails with `IllegalStateException`.

## Shared Config Merge Lifecycle

Before runtime initialization, Graphene merges all registered consumer configs.

- Conflicting explicit `jcefDownloadPath`, `http`, or `remoteDebugging` configs fail startup.
- `extensionFolder` values are merged.
- `fileSystemAccessMode` resolves to `ALLOW` if any consumer requests `ALLOW`; otherwise `DENY`.

## Surface And Widget Lifecycle

- `GrapheneWebViewWidget` owns a `BrowserSurface`.
- Widget creation registers ownership for automatic cleanup.
- `close()` removes widget tracking and closes all surfaces owned by that widget owner key.

`BrowserSurface.close()` performs:

1. owner unregistration
2. load listener scope cleanup
3. bridge detach
4. browser close

## Screen Auto-Close

`ScreenMixin` tracks Graphene web views and closes them by default on `Screen.onClose()`.

On close with auto-close enabled:

1. tracked `GrapheneWebViewWidget` instances are closed
2. surfaces owned by the screen owner key are closed
3. widget tracking is cleared

## Opt Out Of Auto-Close

If you need long-lived surfaces across screen transitions:

```java
import tytoo.grapheneui.api.screen.GrapheneScreens;

GrapheneScreens.setWebViewAutoCloseEnabled(screen, false);
```

When disabled, you must close widgets and surfaces manually.

## Bridge Lifecycle

- On navigation or load start, bridge readiness resets.
- Pending Java requests fail when page changes.
- On load end, Graphene injects bridge scripts.
- During render, Graphene retries bootstrap injection when needed.
- On JS `ready`, queued outbound Java messages flush and `onReady` listeners run.
- On close, bridge listeners/handlers/queue/pending requests are cleared.

## Subscription Lifecycle

Use explicit cleanup for:

- `GrapheneBridgeSubscription`
- `BrowserSurface.Subscription` (load listeners)

Try-with-resources works for both.

---

Next: [Debugging](debugging.md)
