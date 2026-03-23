# Shared Runtime API

Graphene uses a shared-consumer registration model.
This page summarizes the current API shape and merge behavior.

## Registration Model

Each consumer registers before first Graphene usage and receives a scoped `GrapheneHandle`.

```java
GrapheneHandle graphene = GrapheneCore.register(
        MyModClient.class,
        GrapheneConfig.builder()
                .container(GrapheneContainerConfig.builder()
                        .http(GrapheneHttpConfig.builder().randomPortInRange(20_000, 20_050).build())
                        .build())
                .global(GrapheneGlobalConfig.builder()
                        .extensionFolder("./extensions/my-mod")
                        .remoteDebugging(GrapheneRemoteDebugConfig.builder()
                                .randomPort()
                                .allowedOrigins("https://chrome-devtools-frontend.appspot.com")
                                .build())
                        .build())
                .build()
);
```

Entry points:

- `GrapheneCore.register(Class<?> anchorClass)`
- `GrapheneCore.register(Class<?> anchorClass, GrapheneConfig config)`

## Config Split

`GrapheneConfig` is composed of:

- `GrapheneContainerConfig`: per-consumer config
- `GrapheneGlobalConfig`: shared runtime contribution

Typical usage:

- container config: HTTP mount behavior for this mod
- global config: JCEF path, extensions, remote debugging, file-system access mode

## Initialization Timing

- Consumers should register from `onInitializeClient()`.
- Registration closes before the first client tick.
- Automatic startup happens before the first client tick when at least one consumer is registered.
- Lazy startup on first Graphene usage still works if needed.
- If Graphene is used before any consumer is registered, Graphene throws an `IllegalStateException`.

## Merge Rules

When global contributions are merged across consumers:

- `jcefDownloadPath`: explicit values must match
- `remoteDebugging`: explicit values must match
- `fileSystemAccessMode`: `ALLOW` wins over `DENY`
- `extensionFolder`: union merge across consumers

When HTTP container config is merged across consumers:

- `bindHost`, `baseUrlScheme`, and port binding must match
- `fileRoot` stays private to each consumer mount
- `spaFallback` stays private to each consumer mount

Conflicts throw `IllegalStateException` naming the conflicting consumers.

## Handle Helpers

From `GrapheneHandle`:

- `id()`: resolved consumer mod id
- `config()`: full consumer config
- `containerConfig()`: this consumer's container config
- `globalConfig()`: this consumer's contributed global config
- `effectiveGlobalConfig()`: merged global config across all registered consumers
- `runtime()`: shared Graphene runtime view

```java
String appUrl = graphene.appAssets().asset("web/index.html");
String classpathUrl = graphene.classpathAssets().asset("web/index.html");
String classpathHttpUrl = graphene.httpAssets().asset("web/index.html");
String mountedHttpUrl = graphene.httpUrl("web/index.html");
```

Equivalent static forms:

```java
GrapheneAppUrls.assets("my-mod-id").asset("web/index.html");
GrapheneClasspathUrls.assets("my-mod-id").asset("web/index.html");
GrapheneHttpUrls.assets("my-mod-id").asset("web/index.html");
GrapheneHttpUrls.modUrl("my-mod-id", "web/index.html");
```

`GrapheneHttpUrls` requires HTTP mode to be running.
