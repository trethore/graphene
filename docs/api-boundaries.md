# API Boundaries

Date: 2026-07-11

## Purpose

This document records the public API and module boundaries established before migrating the old implementation. These
decisions apply to the `io.github.trethore.graphene` API and all version-specific release artifacts.

## General Rules

- Public common APIs expose only Graphene-owned and JDK types.
- JCEF and jcefgithub remain implementation details under common internal packages.
- Common does not depend on Minecraft, Fabric, Loom, or LWJGL.
- Fabric public APIs may expose Minecraft types when required for normal Minecraft integration.
- Fabric implementations adapt version-specific Minecraft events and rendering APIs to stable common contracts.
- The Fabric mod ID and asset namespace remain `grapheneui`.

## Registration and Context

The main common entry point is `Graphene`. The consumer-scoped object is `GrapheneContext`.
Fabric consumers must register from their `ClientModInitializer` entrypoint. Graphene installs its lightweight platform
backend from its client-only `main` entrypoint before Fabric invokes consumer `client` entrypoints. Native JCEF
installation and runtime startup remain deferred until Minecraft has started and at least one consumer is registered.

```java
GrapheneContext context = Graphene.register(MyModClient.class, config);
```

Explicit mod IDs remain supported for unusual class-loading, embedding, or packaging setups:

```java
GrapheneContext context = Graphene.register("my-mod-id", config);
```

The planned entry-point API is:

```java
public final class Graphene {
    public static GrapheneContext register(Class<?> anchorClass);

    public static GrapheneContext register(Class<?> anchorClass, GrapheneConfig config);

    public static GrapheneContext register(String modId);

    public static GrapheneContext register(String modId, GrapheneConfig config);

    public static GrapheneContext context(Class<?> anchorClass);

    public static GrapheneContext context(String modId);

    public static GrapheneRuntime runtime();
}
```

Class-based registration delegates resolution to a platform-provided mod resolver. If resolution fails, the exception
must recommend explicit string registration.

String registration must:

- validate the mod ID syntax;
- verify through the installed platform service that the mod is currently loaded;
- fail early for misspelled or unavailable IDs.

Registration returns the `GrapheneContext` directly. Context lookup remains available for consumers that do not retain
the returned instance.

## Browser and Surface Model

Common owns a public, Minecraft-independent `BrowserSession`. Fabric owns `BrowserSurface`.

### BrowserSession

`BrowserSession` owns:

- browser lifecycle;
- navigation and history;
- current URL and loading state;
- load events;
- bridge access;
- script execution;
- normalized browser input;
- browser closure.

Consumers may create a session without creating a Fabric surface:

```java
BrowserSession browser = context.browsers().create(options);
```

This supports advanced integrations, custom rendering, automation, and testing.

### BrowserSurface

The Fabric `BrowserSurface` owns:

- one `BrowserSession`;
- viewport and resolution state;
- Minecraft rendering;
- GPU texture management;
- Minecraft cursor conversion;
- Minecraft input adaptation;
- surface ownership integration.

Normal usage creates the session through the surface builder:

```java
BrowserSurface surface =
        BrowserSurface.builder(context)
                .url(url)
                .options(options)
                .build();

BrowserSession browser = surface.browser();
```

A browser session may be attached to at most one surface or render target. Multiple surfaces for one session are
prohibited to avoid resolution, frame ownership, input focus, and lifecycle conflicts.

## Input Boundary

Input translation uses adapters rather than duplicating GLFW-to-JCEF logic in every Minecraft package:

```text
Minecraft version-specific event
    -> Fabric input adapter
    -> Graphene normalized input event
    -> common internal JCEF translator
    -> JCEF browser
```

Fabric extracts information from version-specific Minecraft input types. Common owns stable normalized inputs such as:

```text
BrowserKeyInput
BrowserTextInput
BrowserPointerInput
BrowserScrollInput
BrowserKey
BrowserKeyAction
BrowserModifier
```

Common does not expose or depend on GLFW constants. Fabric maps Minecraft and GLFW values into Graphene types.
Normalized key events retain raw key and scan codes where necessary for unknown keys, keyboard layouts, and
platform-specific behavior.

Common owns shared logic for:

- modifier handling;
- keyboard lock state;
- OS-specific native key resolution;
- Graphene-key-to-JCEF mapping;
- focus state;
- pointer button mapping;
- scroll normalization;
- JCEF event construction.

## Load Events

Load callbacks use explicit Graphene-owned listener methods and event records:

```java
public interface BrowserLoadListener {
    default void onLoadingStateChanged(BrowserLoadingState state) {
    }

    default void onLoadStarted(BrowserLoadStarted event) {
    }

    default void onLoadCompleted(BrowserLoadCompleted event) {
    }

    default void onLoadFailed(BrowserLoadFailed event) {
    }
}
```

Events expose stable information such as:

- URL;
- main-frame status;
- loading and navigation state;
- HTTP status;
- Graphene navigation type;
- Graphene error code and message.

No `org.cef` type may appear in these APIs.

## Asset Identifiers

Common introduces a Graphene-owned `AssetId` value type:

```java
AssetId asset = AssetId.of("my-mod", "web/index.html");
String url = context.assets().url(asset);
```

Convenience overloads accepting namespace and path strings remain available. Common does not use Minecraft's
`Identifier`.

## Runtime Configuration Terminology

Public configuration uses implementation-neutral terminology. JCEF-specific names such as `jcefDownloadPath` are
replaced with names such as:

```java
browserRuntimePath(...)
```

The final name should describe the installed browser runtime without promising a specific browser implementation.

## Browser Options

Common exposes a stable `BrowserOptions` type rather than JCEF settings or customizers:

```java
BrowserOptions options =
        BrowserOptions.builder()
                .maximumFrameRate(60)
                .transparent(true)
                .javascriptEnabled(true)
                .build();
```

The initial options should cover stable behavior Graphene can support independently of JCEF, including:

- maximum frame rate;
- transparency;
- background color;
- JavaScript enablement.

Raw `CefBrowserSettings`, `CefRequestContext`, `CefClient`, and arbitrary JCEF customizers are not exposed. Unsupported
implementation-specific settings remain internal.

## Runtime API

The runtime API uses Graphene-owned state and values. It must not expose JCEF objects.

Planned concepts include:

- explicit runtime state;
- asynchronous initialization status;
- optional remote-debugging port;
- HTTP server status;
- controlled shutdown owned by the platform lifecycle.

## Enforcement

Architecture verification must enforce:

1. Common contains no imports from Minecraft, Fabric, or LWJGL.
2. Public common packages contain no imports from JCEF or jcefgithub.
3. Fabric code contains no imports from JCEF or jcefgithub.
4. Public APIs do not expose classes from internal packages.
5. JCEF access is restricted to common internal adapter packages.

## Confirmed Decisions

| Area                     | Decision                                          |
|--------------------------|---------------------------------------------------|
| Main entry point         | `Graphene`                                        |
| Consumer-scoped object   | `GrapheneContext`                                 |
| Registration             | Class and explicit string overloads               |
| Explicit ID validation   | Must identify a currently loaded mod              |
| Browser core             | Public common `BrowserSession`                    |
| Browser rendering        | Fabric `BrowserSurface`                           |
| Headless/custom sessions | Publicly creatable through the context            |
| Surface attachment       | At most one surface/render target per session     |
| Input translation        | Version-specific adapter plus shared common logic |
| Load events              | Explicit Graphene event records and callbacks     |
| Assets                   | Graphene-owned `AssetId`                          |
| Runtime path terminology | Implementation-neutral                            |
| Browser configuration    | Stable Graphene-owned `BrowserOptions`            |
| Raw JCEF access          | Not public                                        |
