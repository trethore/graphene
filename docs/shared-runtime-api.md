# Shared Runtime API

Graphene uses a shared-consumer registration model.
This page summarizes the behavior and migration impact for integrations that assumed single-consumer initialization.

## What Changed

Older first-call-wins startup behavior has been replaced.
Now each consumer registers first, then Graphene merges config and initializes one shared runtime.

```java
GrapheneMod graphene = GrapheneCore.register("my-mod-id", GrapheneConfig.builder()
        .http(GrapheneHttpConfig.builder().randomPortInRange(20_000, 20_050).build())
        .extensionFolder("./extensions/my-mod")
        .remoteDebugging(GrapheneRemoteDebugConfig.builder()
                .randomPort()
                .allowedOrigins("https://chrome-devtools-frontend.appspot.com")
                .build())
        .build());
```

Entry points:

- `GrapheneCore.register(String modId)`
- `GrapheneCore.register(String modId, GrapheneConfig config)`

`register(...)` returns `GrapheneMod`, which includes namespace-bound URL helpers.

## Initialization Timing

- Automatic startup after client start when at least one consumer is registered.
- Lazy startup on first Graphene usage if needed.
- If Graphene is used before any consumer is registered, Graphene throws an initialization error.

## Conflict Rules

When configs are merged across consumers:

- `jcefDownloadPath`: explicit values must match
- `http`: explicit values must match
- `remoteDebugging`: explicit values must match
- `fileSystemAccessMode`: `ALLOW` wins over `DENY`
- `extensionFolder`: union merge across consumers

Conflicts throw `IllegalStateException` naming the conflicting consumers.

Normalization notes:

- `jcefDownloadPath` is compared as normalized absolute path.
- `GrapheneHttpConfig.fileRoot` is normalized to absolute path, so equivalent relative/absolute references compare equal.

## Namespace-Bound URL Helpers

From `GrapheneMod`:

```java
GrapheneMod graphene = GrapheneCore.register("my-mod-id");

String appUrl = graphene.appAssets().asset("web/index.html");
String classpathUrl = graphene.classpathAssets().asset("web/index.html");
String httpUrl = graphene.httpAssets().asset("web/index.html");
```

Equivalent static forms:

```java
GrapheneAppUrls.assets("my-mod-id").asset("web/index.html");
GrapheneClasspathUrls.assets("my-mod-id").asset("web/index.html");
GrapheneHttpUrls.assets("my-mod-id").asset("web/index.html");
```

`GrapheneHttpUrls` requires HTTP mode to be running.
