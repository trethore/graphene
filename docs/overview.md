# Graphene Overview

Graphene is a client-side UI library for Fabric Minecraft mods. It embeds Chromium via JCEF, renders HTML/CSS/JS in
Minecraft screens, and exposes a Java <-> JavaScript bridge.

![Graphene demo](images/demo.png)

**Main API**

- `GrapheneCore` - registration and runtime entry point
- `GrapheneHandle` - consumer-scoped handle returned by `GrapheneCore.register(...)`
- `GrapheneWebViewWidget` - screen widget wrapper around a browser surface
- `BrowserSurface` - direct off-screen browser surface
- `GrapheneBridge` - Java <-> JavaScript event and request messaging

## Core Concepts

| Concept                   | Purpose                                                                                                 |
|---------------------------|---------------------------------------------------------------------------------------------------------|
| `GrapheneConfig`          | Top-level config split into container and global sections                                               |
| `GrapheneContainerConfig` | Per-consumer config, such as HTTP mount behavior                                                        |
| `GrapheneGlobalConfig`    | Shared runtime contribution, such as JCEF path, remote debugging, extensions, and file access           |
| `GrapheneRuntime`         | Runtime state and helpers (`isInitialized`, remote debugging port, DevTools opening, HTTP server state) |
| `GrapheneHttpServer`      | Runtime HTTP server view (`isRunning`, `host`, `port`, `baseUrl`)                                       |

## Runtime Model

Graphene uses a shared-runtime registration model:

1. Each mod registers once with `GrapheneCore.register(MyModClient.class)` or
   `GrapheneCore.register(MyModClient.class, config)`.
2. Graphene resolves the Fabric mod id from the anchor class.
3. Graphene merges all registered global config contributions.
4. Runtime initialization happens:
    - automatically after startup before the first client tick when at least one consumer is registered
    - lazily on first Graphene usage if startup has not initialized it yet

If nothing is registered and Graphene is used, Graphene throws an `IllegalStateException` asking you to register first.

## Typical Flow

1. Register your mod in `ClientModInitializer` and store the returned `GrapheneHandle`.
2. Create a `GrapheneWebViewWidget` or `BrowserSurface`.
3. Load a page from `app://assets/...`, `classpath:///assets/...`, the HTTP `/assets/...` route, or your mounted
   `/mods/<mod-id>/...` route.
4. Bridge bootstrap scripts are injected.
5. JavaScript sends `ready`.
6. Java and JS exchange events and requests.

## Next

| Need                          | Page                                    |
|-------------------------------|-----------------------------------------|
| Setup and dependency wiring   | [Installation](installation.md)         |
| First screen integration      | [Quickstart](quickstart.md)             |
| Java <-> JS contracts         | [Bridge](bridge.md)                     |
| Rendering and sizing controls | [Advanced Surface](advanced-surface.md) |
