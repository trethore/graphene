# Architecture and Runtime

Graphene separates a loader-independent browser API from Minecraft-specific rendering and input integration.

## Process overview

```text
Consumer mod
  -> Graphene public API
    -> Fabric integration
      -> shared JCEF runtime
        -> off-screen Chromium browsers
```

The common API does not expose JCEF, Fabric, Minecraft, or LWJGL types. Fabric-specific APIs turn browser frames into
Minecraft textures and translate game input into browser input.

## Registration creates a consumer context

Each consuming mod registers once:

```java
GrapheneContext context = Graphene.register(ConsumerClient.class, config);
```

The context contains:

- The resolved consumer mod ID.
- The consumer's original configuration.
- App, classpath, and HTTP asset URL factories.
- A browser-session factory.
- Access to the shared runtime.

Asset URLs created through a context default to that consumer's mod namespace.

## One shared runtime

Graphene installs one process-wide JCEF runtime rather than one Chromium installation per consuming mod. The runtime
owns native initialization, browser process configuration, DevTools discovery, and the optional shared HTTP server.

The runtime has these states:

```text
NEW -> STARTING -> RUNNING -> STOPPING -> STOPPED
                   |
                   -> FAILED
```

Registration is accepted before startup. Once startup begins, registration closes so Graphene can resolve one stable
global configuration.

`GrapheneRuntime.initialization()` completes when the runtime first reaches `RUNNING`, or exceptionally when startup
fails or stops first. Browser sessions can be created only while the runtime is `RUNNING`.

## Consumer and global configuration

`GrapheneConfig` has two parts:

- `GrapheneContainerConfig` belongs to one consumer. It currently controls that consumer's HTTP asset mount.
- `GrapheneGlobalConfig` contributes settings to the shared process-wide browser runtime.

Some global values must be identical across consumers:

- Browser runtime path.
- Remote-debug configuration.
- Browser file-access policy.

Extension folders are combined. Conflicting exclusive values throw `GrapheneGlobalConfigConflictException` during
registration, before native startup makes the conflict harder to diagnose.

The configuration returned by `context.globalConfig()` is the consumer's contribution. `context.effectiveGlobalConfig()`
is the resolved process-wide result.

## Optional shared HTTP server

The HTTP server starts only when at least one consumer enables `GrapheneHttpConfig`. It binds to loopback and hosts
separate mounts for each consumer. Shared bind and port settings must agree, while each mount can use its own filesystem
override and SPA fallback.

## Lifecycle ownership

The Fabric platform starts Graphene after consumer registration and stops it with the client. Consumers own the browser
objects they create, while Graphene owns the shared native runtime.

This separation lets a mod close a screen or browser without affecting other Graphene consumers.

## Related documentation

- [Browser layers](browser-layers.md)
- [Manage browser lifecycle](../how-to/manage-browser-lifecycle.md)
- [Configuration and defaults](../reference/configuration-and-defaults.md)
