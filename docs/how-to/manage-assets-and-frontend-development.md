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

This produces `app://assets/<mod-id>/ui/index.html`. The context defaults the asset namespace to the consumer's mod ID,
and relative links inside the page work normally.

The namespace is part of the URL path. Graphene's bridge-origin checks treat all generated app asset URLs as the same
`app://assets` origin; use bridge policies and Java handler design accordingly when multiple mods contribute content.

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

After runtime initialization completes, create an HTTP URL for a packaged resource:

```java
String url = context.httpUrl("ui/index.html");
```

`context.httpUrl(...)` and `context.httpAssets().url(...)` require the shared HTTP server to be running. They throw
`IllegalStateException` when HTTP is disabled or runtime initialization has not completed.

The shared server binds to loopback. Its final host and port are also available after initialization:

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

## Serve video assets

Graphene serves `.webm` and `.mp4` assets with their standard MIME types and supports single byte-range requests for
HTTP, app, and classpath resources. MIME and range support do not guarantee that the bundled browser runtime can decode
every codec used by those containers.

Use WebM with VP8 or VP9 for video intended to work with Graphene's default JCEF runtime. MP4 assets are served
correctly, but H.264 and AAC playback is not guaranteed. The default Chromium-derived runtime does not automatically
use an operating system H.264 installation when its build excludes proprietary codecs. A separately built JCEF runtime
may provide additional codec support.

Browser code can select a source based on runtime capabilities:

```javascript
const video = document.createElement("video");
const supportsVp9 = video.canPlayType('video/webm; codecs="vp9"') !== "";
const supportsH264 = video.canPlayType('video/mp4; codecs="avc1.42E01E"') !== "";
```

For packaged assets, convert H.264 MP4 input to VP9 WebM during the frontend build when broad compatibility with the
default runtime is required:

```bash
ffmpeg -i input.mp4 -c:v libvpx-vp9 -crf 32 -b:v 0 -c:a libopus output.webm
```

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
