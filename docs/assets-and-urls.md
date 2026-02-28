# Assets And URLs

Graphene provides URL helpers to load web assets from your mod namespace and optional runtime HTTP server.

## URL Helpers

App scheme (`app://`):

- `GrapheneAppUrls.asset(namespace, path)`
- `grapheneMod.appAssets().asset(path)`

Classpath scheme (`classpath:///`):

- `GrapheneClasspathUrls.asset(namespace, path)`
- `grapheneMod.classpathAssets().asset(path)`

Runtime HTTP (from configured base URL scheme, default `http://`):

- `GrapheneHttpUrls.asset(namespace, path)`
- `grapheneMod.httpAssets().asset(path)`

Examples:

```java
String appUrl = GrapheneAppUrls.asset("my-mod-id", "web/index.html");
// app://assets/my-mod-id/web/index.html

String classpathUrl = GrapheneClasspathUrls.asset("my-mod-id", "web/index.html");
// classpath:///assets/my-mod-id/web/index.html
```

## Resource Layout

```text
src/client/resources/
  assets/
    my-mod-id/
      web/
        index.html
        app.js
        styles.css
        images/logo.png
```

Load with:

```java
String url = GrapheneAppUrls.asset("my-mod-id", "web/index.html");
```

## Relative Paths Inside HTML

If your page is loaded from `app://assets/my-mod-id/web/index.html`, relative references resolve naturally:

```html
<link rel="stylesheet" href="styles.css">
<script src="app.js"></script>
<img src="images/logo.png" alt="logo">
```

## URL Normalization

`GrapheneAppUrls.normalizeResourcePath(url)` and `GrapheneClasspathUrls.normalizeResourcePath(url)`:

- verify scheme
- strip query and fragment
- decode URL-encoded path segments
- return normalized classpath resource path

Example:

- input: `app://assets/graphene-ui/a%20b.html?x=1#top`
- result: `assets/graphene-ui/a b.html`

## MIME Types

Graphene resolves common MIME types for HTML, CSS, JS, JSON, images, fonts, and WASM.
Unknown extensions default to `text/plain`.

## HTTP Mode

Enable HTTP mode with `GrapheneHttpConfig`:

```java
GrapheneConfig config = GrapheneConfig.builder()
        .http(GrapheneHttpConfig.builder()
                .bindHost("127.0.0.1")
                .randomPortInRange(20_000, 21_000)
                .fileRoot("C:/dev/my-ui-dist")
                .spaFallback("/assets/my-mod-id/web/index.html")
                .build())
        .build();

GrapheneCore.register("my-mod-id", config);

String url = GrapheneHttpUrls.asset("my-mod-id", "web/index.html");
```

Important behavior:

- `GrapheneHttpUrls.asset(...)` throws `IllegalStateException` when HTTP server is not running.
- HTTP request resolution order:
1. filesystem (`fileRoot/request-path`)
2. classpath assets
3. optional SPA fallback for non-`/assets/...` `GET` and `POST` requests

## Recommendations

- Keep web assets under `assets/<mod-id>/web/...`.
- Use namespaced helper APIs instead of hard-coded string concatenation.
- Keep filenames lowercase and extension-explicit.
- In shared runtime setups, keep HTTP paths namespaced (`/assets/<mod-id>/...`).

---

Next: [Lifecycle](lifecycle.md)
