# Assets And URLs

Graphene registers custom schemes so browser pages can load resources directly from mod jars.

## URL Forms

Recommended:

- `app://assets/<mod-id>/...`
- `GrapheneAppUrls.asset("<mod-id>", "...")` for mod-owned assets under `assets/<mod-id>/...`

Examples:

```java
String grapheneDebugAsset = GrapheneAppUrls.asset("graphene-ui-debug", "graphene_test/welcome.html");
// app://assets/graphene-ui-debug/graphene_test/welcome.html

String myModAsset = GrapheneAppUrls.asset("my-mod-id", "web/index.html");
// app://assets/my-mod-id/web/index.html
```

## Where To Put Files

Typical layout in a consumer mod:

```text
src/client/resources/
  assets/
    my-mod-id/
      web/
        index.html
        app.js
        styles.css
        images/
          logo.png
```

Then load with:

```java
String url = GrapheneAppUrls.asset("my-mod-id", "web/index.html");
```

## Relative Resource Loading

If your page is loaded from `app://assets/my-mod-id/web/index.html`, then relative paths inside HTML work naturally:

```html
<link rel="stylesheet" href="styles.css">
<script src="app.js"></script>
<img src="images/logo.png" alt="logo">
```

## Path Normalization Notes

Graphene normalizes asset URLs by:

- stripping query string and fragment
- decoding URL-encoded segments
- trimming leading slashes

Example:

`app://assets/graphene-ui/a%20b.html?x=1#top`

resolves to resource path:

`assets/graphene-ui/a b.html`

## MIME Types

The classpath scheme handler maps common extensions, including:

- HTML/CSS/JS/JSON
- PNG/JPG/GIF/WEBP/SVG/ICO
- WOFF/WOFF2/TTF/OTF
- WASM

Unknown extensions default to `text/plain`.

## Best Practices

- Keep all web resources under one folder (`assets/<mod-id>/web/...`).
- Prefer lowercase file names and explicit extensions.
- Prefer `GrapheneAppUrls.asset("<mod-id>", "...")` for your mod namespace.
- Use an explicit namespace for bundled samples too (for example `graphene-ui-debug`).

## Two Schemes

- `GrapheneAppUrls` (`app://assets/...`) targets framework-heavy pages that need browser-like origin behavior.
- `GrapheneClasspathUrls` (`classpath:///assets/...`) stays available for simple classpath file loading.

## Loopback HTTP URLs

Enable HTTP mode with `GrapheneHttpConfig`, then build runtime HTTP URLs:

```java
GrapheneConfig config = GrapheneConfig.builder()
        .http(GrapheneHttpConfig.builder()
                .bindHost("127.0.0.1")
                .randomPortInRange(20_000, 21_000)
                .spaFallback("/assets/my-mod-id/web/index.html")
                .build())
        .build();

GrapheneCore.init(config);

String url = GrapheneHttpUrls.asset("my-mod-id", "web/index.html");
// http://127.0.0.1:<port>/assets/my-mod-id/web/index.html
```

---
Next: [Lifecycle](lifecycle.md)
