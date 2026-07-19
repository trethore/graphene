# Manage Assets and Frontend Development

Graphene can load packaged resources directly or expose them through a loopback HTTP server. Start with packaged app
assets and enable HTTP hosting only when a frontend workflow needs it.

## Load packaged app assets

Place consumer-owned files under your mod's resource namespace:

```text
src/main/resources/assets/<mod-id>/ui/index.html
src/main/resources/assets/<mod-id>/ui/style.css
src/main/resources/assets/<mod-id>/ui/app.js
```

Create a URL through the registered context:

```java
String url = context.appAssets().url("ui/index.html");
```

This produces an `app://` URL scoped to the consumer's mod ID. Relative links inside the page work normally.

Use another namespace only when you intentionally need an asset owned by another loaded resource namespace:

```java
String url = context.appAssets().url("othermod", "ui/shared.html");
```

## Load classpath assets

`context.classpathAssets()` creates `classpath://` URLs backed by classloader resources:

```java
String url = context.classpathAssets().url("ui/index.html");
```

Prefer `appAssets()` for normal mod interfaces. Use classpath URLs when code specifically needs the public classpath
scheme or a URL independent of a registered app origin.

## Enable the HTTP asset mount

The HTTP mount is disabled by default. Enable it during registration:

```java
GrapheneHttpConfig http = GrapheneHttpConfig.builder().build();

GrapheneConfig config =
        GrapheneConfig.builder()
                .container(GrapheneContainerConfig.builder().http(http).build())
                .build();

GrapheneContext context = Graphene.register(ExampleModClient.class, config);
```

Create an HTTP URL for a packaged resource:

```java
String url = context.httpUrl("ui/index.html");
```

The shared server binds to loopback. Its final host and port are available after initialization:

```java
GrapheneHttpServer server = context.runtime().httpServer();
```

## Serve frontend files from disk during development

Use `fileRoot` to serve files from a frontend output directory before falling back to packaged resources:

```java
GrapheneHttpConfig http =
        GrapheneHttpConfig.builder()
                .fileRoot("../frontend/dist")
                .spaFallback("index.html")
                .build();
```

With this configuration:

1. A requested file under `../frontend/dist` wins.
2. Missing files fall back to packaged assets under your mod namespace.
3. A missing route falls back to `index.html`, which supports client-side SPA routing.

Only enable `fileRoot` in a development configuration. Package production frontend output under `assets/<mod-id>/...` so
released users do not depend on external files.

## Keep browser code independent of the source

Choose the initial URL in Java:

```java
String pageUrl = developmentMode
        ? context.httpUrl("index.html")
        : context.appAssets().url("ui/index.html");
```

The browser widget, bridge channels, and page code can remain the same in both modes.

## Use frameworks and browser libraries

Graphene does not require a frontend framework or bundler. React, Vue, Svelte, Three.js, and similar libraries work when
their output can run in Chromium as ordinary HTML, CSS, JavaScript, and assets.

Configure the frontend build for relative asset paths or for the path exposed by `context.httpUrl(...)`. Emit production
files into your mod's resources before building the release JAR.

## Shared HTTP settings

All consumers with HTTP enabled share one loopback server. Their `bindHost` and port-selection settings must agree. Each
consumer still has its own asset mount, `fileRoot`, and SPA fallback.

Use the default random range unless a fixed port is required:

```java
GrapheneHttpConfig.builder()
    .randomPortInRange(20_000, 21_000)
    .build();
```

See [configuration and defaults](../reference/configuration-and-defaults.md) for the complete settings.

## Next steps

- [Enable Chromium DevTools](use-devtools.md).
- [Understand asset origins and bridge exposure](../explanation/assets-origins-and-bridge-security.md).
- [Troubleshoot blank pages and HTTP startup](troubleshoot.md).
