# Graphene Overview

Graphene is a client-side UI library for Fabric mods on Minecraft `1.21.11`.
It embeds Chromium via JCEF, so you can render HTML/CSS/JS in Minecraft screens and communicate with Java code through a bridge.

![Graphene demo](images/demo.png)

## Core Concepts

- `GrapheneCore`: registration and runtime entry point
- `GrapheneMod`: namespace-bound helper returned by `GrapheneCore.register(...)`
- `GrapheneRuntime`: runtime state (`isInitialized`, remote debugging port, HTTP server state)
- `GrapheneHttpServer`: runtime HTTP server view (`isRunning`, `host`, `port`, `baseUrl`)
- `BrowserSurface`: off-screen browser surface with rendering, navigation, sizing, and input mapping
- `GrapheneWebViewWidget`: Minecraft widget wrapper around `BrowserSurface`
- `GrapheneBridge`: Java <-> JS event/request messaging

## Runtime Model

Graphene uses shared consumer registration:

1. Each mod registers once with `GrapheneCore.register(modId)` or `GrapheneCore.register(modId, config)`.
2. Graphene merges all registered configs.
3. Runtime initialization happens:
- automatically after client startup if at least one consumer is registered
- lazily on first Graphene usage if startup did not initialize it yet

If nothing is registered and Graphene is used, Graphene throws an `IllegalStateException` asking you to register first.

## Typical Flow

1. Register your mod in `ClientModInitializer`.
2. Create a `GrapheneWebViewWidget` or `BrowserSurface`.
3. Load a page from `app://assets/...`, `classpath:///assets/...`, or HTTP mode.
4. Bridge bootstrap scripts are injected.
5. JavaScript sends `ready`.
6. Java and JS exchange events and requests.

## Next

- Setup and dependency wiring: [Installation](installation.md)
- First screen integration: [Quickstart](quickstart.md)
- Java <-> JS contracts: [Bridge](bridge.md)
- Rendering and sizing controls: [Advanced Surface](advanced-surface.md)

---

Next: [Installation](installation.md)
