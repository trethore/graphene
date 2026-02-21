# Shared Runtime API (Breaking)

Graphene now uses a shared-consumer registration model instead of immediate runtime startup.

## Why

When multiple mods called `GrapheneCore.register(modId, GrapheneConfig)` with different values, the first call won and later calls were ignored.
That created hidden conflicts for HTTP setup, extension folders, and JCEF install path selection.

## New flow

1. Each mod registers itself during client init.
2. Graphene merges all registered configs.
3. Runtime initializes automatically once client loading has finished.

## New entry points

```java
GrapheneMod graphene = GrapheneCore.register("my-mod-id", GrapheneConfig.builder()
        .http(GrapheneHttpConfig.builder().randomPortInRange(20_000, 20_050).build())
        .extensionFolder("./extensions/my-mod")
        .build());
```

- `GrapheneCore.register(String modId)` registers with defaults.
- `GrapheneCore.register(String modId, GrapheneConfig config)` registers with explicit shared-runtime requirements.
- Runtime startup is automatic after client loading completes.

## Conflict model

- `jcefDownloadPath`: all explicit values must match.
- `http`: all explicit values must match.
- `extensionFolder`: values are merged (union).

`GrapheneHttpConfig.fileRoot(...)` is part of HTTP config equality. Graphene normalizes it to an absolute path, so equivalent
relative and absolute references to the same directory are treated as the same shared setting.

If conflicting shared settings are registered, Graphene throws a clear `IllegalStateException` during startup.

## Namespace-safe URL helpers

You can now bind URL helpers to a mod namespace:

```java
GrapheneMod graphene = GrapheneCore.register("my-mod-id");
String appUrl = graphene.appAssets().asset("web/index.html");
String classpathUrl = graphene.classpathAssets().asset("web/index.html");
String httpUrl = graphene.httpAssets().asset("web/index.html");
```

Equivalent static helpers are available:

```java
GrapheneAppUrls.assets("my-mod-id").asset("web/index.html");
GrapheneClasspathUrls.assets("my-mod-id").asset("web/index.html");
GrapheneHttpUrls.assets("my-mod-id").asset("web/index.html");
```
