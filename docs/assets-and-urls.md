# Assets And URLs

Graphene registers a custom `classpath` scheme so browser pages can load resources directly from mod jars.

## URL Forms

Recommended:

- `classpath:///assets/<mod-id>/...`
- `GrapheneClasspathUrls.asset("<mod-id>", "...")` for mod-owned assets under `assets/<mod-id>/...`

Examples:

```java
String grapheneDebugAsset = GrapheneClasspathUrls.asset("graphene-ui-debug", "graphene_test/welcome.html");
// classpath:///assets/graphene-ui-debug/graphene_test/welcome.html

String myModAsset = GrapheneClasspathUrls.asset("my-mod-id", "web/index.html");
// classpath:///assets/my-mod-id/web/index.html
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
String url = GrapheneClasspathUrls.asset("my-mod-id", "web/index.html");
```

## Relative Resource Loading

If your page is loaded from `classpath:///assets/my-mod-id/web/index.html`, then relative paths inside HTML work naturally:

```html
<link rel="stylesheet" href="styles.css">
<script src="app.js"></script>
<img src="images/logo.png" alt="logo">
```

## Path Normalization Notes

Graphene normalizes classpath URLs by:

- stripping query string and fragment
- decoding URL-encoded segments
- trimming leading slashes

Example:

`classpath:///assets/graphene-ui/a%20b.html?x=1#top`

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
- Prefer `GrapheneClasspathUrls.asset("<mod-id>", "...")` for your mod namespace.
- Use an explicit namespace for bundled samples too (for example `graphene-ui-debug`).

---
Next: [Lifecycle](lifecycle.md)
