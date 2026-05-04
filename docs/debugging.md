# Debugging

This page covers the most useful Graphene diagnostics for integration work.

**Diagnostic tools**

| Tool             | Use for                                           |
|------------------|---------------------------------------------------|
| Remote DevTools  | Inspect page DOM, console, network, and JS errors |
| Debug screen     | Exercise bundled Graphene pages in-game           |
| `graphene.debug` | Enable targeted Graphene DEBUG logs               |
| Runtime helpers  | Query DevTools target URLs and runtime state      |

## Remote DevTools

Remote debugging is disabled unless configured.

```java
GrapheneConfig config = GrapheneConfig.builder()
        .global(GrapheneGlobalConfig.builder()
                .remoteDebugging(GrapheneRemoteDebugConfig.builder()
                        .randomPort()
                        .allowedOrigins("*")
                        .build())
                .build())
        .build();

GrapheneCore.register(MyModClient.class, config);
```

Open DevTools for a `BrowserSurface`:

```java
GrapheneCore.runtime().openDevTools(surface)
        .exceptionally(throwable -> {
            LOGGER.warn("Failed to open Graphene DevTools", throwable);
            return null;
        });
```

If you only need the URI, resolve it without opening the platform browser:

```java
GrapheneCore.runtime().resolveDevToolsUri(surface)
        .thenAccept(uri -> LOGGER.info("Graphene DevTools: {}", uri));
```

Runtime inspection is still available:

```java
int port = GrapheneCore.runtime().getRemoteDebuggingPort();
// -1 when disabled, > 0 when enabled
```

The runtime DevTools helpers query CEF's local target list, pick the matching page target for the surface, and build a
local inspector URL.
You do not need to manually open `http://127.0.0.1:<port>/json` for normal use.

![DevTools targets page](images/devtools-targets.png)

## Repository Debug Screen

In this repository's debug module:

- Press `F10` to open `GrapheneBrowserDebugScreen`
- Use navigation controls and the `DevTools` button
- Test bridge and page behavior with bundled pages

Useful bundled sample pages:

- `app://assets/graphene-ui-debug/graphene_test/pages/welcome.html`
- `app://assets/graphene-ui-debug/graphene_test/pages/tests.html`
- `app://assets/graphene-ui-debug/graphene_test/pages/js-bridge.html`
- `app://assets/graphene-ui-debug/graphene_test/pages/automated-tests.html`

Classpath equivalents also work.

![Debug screen overview](images/debug-screen-overview.png)

## Debug Runs

For repository integration work, use the debug client run configuration:

```bash
./gradlew :fabric-1.21.11:runDebugClient
```

Then in-game:

- press `F10` to open `GrapheneBrowserDebugScreen`
- use the bundled pages to validate loading, navigation, and bridge behavior
- use the `DevTools` button if remote debugging is enabled for the current Graphene runtime

To enable Graphene debug logs for the repository debug run:

```bash
./gradlew :fabric-1.21.11:runDebugClient -PgrapheneDebug=*
./gradlew :fabric-1.21.11:runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.bridge
./gradlew :fabric-1.21.11:runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime
./gradlew :fabric-1.21.11:runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.cef,tytoo.grapheneuidebug
```

To disable Graphene debug logs again, run without `-PgrapheneDebug`.

When `-PgrapheneDebug=...` is set for the version module's `runDebugClient`, the run configuration also enables `fabric.log.level=debug`.

## Logging

Graphene supports debug selectors via the JVM property `graphene.debug`.

Examples:

- `-Dgraphene.debug=*`
- `-Dgraphene.debug=tytoo.grapheneui.internal.bridge`
- `-Dgraphene.debug=tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime`
- `-Dgraphene.debug=tytoo.grapheneui.internal.cef,tytoo.grapheneuidebug`

Selector behavior:

- `*` enables all Graphene debug logs
- a fully qualified class name enables logs for that exact class
- a package prefix enables logs for that package and its subpackages
- comma-separated selectors enable multiple classes and packages at once
- blank or missing values disable Graphene debug logs

For non-repository launch setups, pass `-Dgraphene.debug=...` to the JVM and ensure your logging backend is configured
to emit DEBUG-level logs.

## Quick Checks

- `GrapheneCore.isInitialized()` is `true` after runtime startup.
- `GrapheneCore.runtime().isInitialized()` is `true` when runtime reports ready.
- `globalThis.grapheneBridge` exists in page console.
- `bridge.onReady(...)` fires after page bridge bootstrap finishes.

## API note

`GrapheneCore.LOGGER` is not public API.
Use your own class-local SLF4J logger in consumer code.
