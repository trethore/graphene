# Debugging

This page covers the most useful runtime diagnostics when integrating Graphene.

## Open Chromium DevTools

Graphene enables remote debugging on a runtime-selected port.

```java
int port = GrapheneCore.runtime().getRemoteDebuggingPort();
String endpoint = "http://127.0.0.1:" + port + "/json";
```

Open the `/json` endpoint in a browser to inspect available targets, then open DevTools for your page target.

![DevTools targets page](images/devtools-targets.png)

## Use The Repository Debug Screen (This Repo)

In this repository's debug mod:

- press `F10` to open `GrapheneBrowserDebugScreen`
- use the `DevTools` button to open the remote debug endpoint
- test bridge events/requests from the sample pages

Recommended sample pages:

- `classpath:///assets/graphene-ui-debug/graphene_test/pages/welcome.html`
- `classpath:///assets/graphene-ui-debug/graphene_test/pages/tests.html`

![Debug screen overview](images/debug-screen-overview.png)

## Logging Tips

- Watch Graphene logs for initialization status and CEF startup args.
- Bridge and load listener exceptions are logged with context.
- Enable package-scoped debug traces in this repository with `runDebugClient`:
  - `./gradlew runDebugClient -PgrapheneDebug=*`
  - `./gradlew runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.cef`
  - `./gradlew runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.bridge,tytoo.grapheneuidebug`
- `grapheneDebug` selector semantics:
  - `*` enables all Graphene debug logs.
  - comma-separated package prefixes enable matching namespaces only.
  - prefixes match exact package or subpackages.
- When `-PgrapheneDebug=...` is set for `runDebugClient`, the run config now also enables `fabric.log.level=debug` so SLF4J debug lines are visible in console output.
- Best setup for Graphene consumers (non-repo mods):
  - pass a startup JVM property such as `-Dgraphene.debug=tytoo.grapheneui.internal.bridge` (or another package prefix).
  - configure your logger backend to DEBUG only for `tytoo.grapheneui` so you do not enable global debug noise.
  - Log4j2 example (`log4j2.xml`):

```xml
<Loggers>
    <Logger name="tytoo.grapheneui" level="debug"/>
    <Root level="info"/>
</Loggers>
```

- restart the game after changing `graphene.debug` because selector parsing happens at startup.
- If page interactions fail silently, inspect both:
  - Minecraft log output
  - browser console in DevTools

## Migration Note

- `GrapheneCore.LOGGER` has been removed as an intentional API break in this repository.
- Use class-local SLF4J loggers (`LoggerFactory.getLogger(CurrentClass.class)`) in consumer code.

## Quick Checks

- `GrapheneCore.isInitialized()` should be `true` after startup.
- `GrapheneCore.runtime().getRemoteDebuggingPort()` should be `> 0`.
- `bridge.onReady(...)` should fire after page load.
- `globalThis.grapheneBridge` should exist in page console.

If those image files do not exist yet, capture and add them under `docs/images/`.

---

Next: [Advanced Surface](advanced-surface.md)
