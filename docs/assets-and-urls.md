# Assets And URLs

Graphene provides handle-scoped URL helpers for loading web assets from your mod namespace and, optionally, from the runtime HTTP server.

## URL Helpers

App scheme (`app://`):

- `grapheneHandle.appAssets().asset(path)`

Classpath scheme (`classpath:///`):

- `GrapheneClasspathUrls.asset(namespace, path)`
- `grapheneHandle.classpathAssets().asset(path)`

Runtime HTTP for shared classpath assets:

- `grapheneHandle.httpAssets().asset(path)`

Runtime HTTP for this mod's mounted container route:

- `grapheneHandle.httpUrl(path)`

Examples:

```java
String appUrl = grapheneHandle.appAssets().asset("web/index.html");
// app://assets/my-mod-id/web/index.html

String classpathUrl = GrapheneClasspathUrls.asset("my-mod-id", "web/index.html");
// classpath:///assets/my-mod-id/web/index.html

String classpathHttpUrl = grapheneHandle.httpAssets().asset("web/index.html");
// http://127.0.0.1:<port>/assets/my-mod-id/web/index.html

String mountedHttpUrl = grapheneHandle.httpUrl("web/index.html");
// http://127.0.0.1:<port>/mods/my-mod-id/web/index.html
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
String url = grapheneHandle.appAssets().asset("web/index.html");
```

## Relative Paths Inside HTML

If your page is loaded from `app://assets/my-mod-id/web/index.html`, relative references resolve naturally:

```html
<link rel="stylesheet" href="styles.css">
<script src="app.js"></script>
<img src="images/logo.png" alt="logo">
```

## URL Normalization

Graphene normalizes `app://` and `classpath:///` asset requests before resolving classpath resources.
`GrapheneClasspathUrls.normalizeResourcePath(url)` is also available when you need to normalize a public classpath URL yourself:

- verify scheme
- strip query and fragment
- decode URL-encoded path segments
- return normalized classpath resource path

Example:

- input: `classpath:///assets/graphene-ui/a%20b.html?x=1#top`
- result: `assets/graphene-ui/a b.html`

## MIME Types

Graphene resolves common MIME types for HTML, CSS, JS, JSON, images, fonts, and WASM.
Unknown extensions default to `text/plain`.

## HTTP Mode

Enable HTTP mode with `GrapheneContainerConfig`:

```java
GrapheneConfig config = GrapheneConfig.builder()
        .container(GrapheneContainerConfig.builder()
                .http(GrapheneHttpConfig.builder()
                        .bindHost("127.0.0.1")
                        .randomPortInRange(20_000, 21_000)
                        .fileRoot("C:/dev/my-ui-dist")
                        .spaFallback("/assets/my-mod-id/web/index.html")
                        .build())
                .build())
        .build();

GrapheneCore.register(MyModClient.class, config);

GrapheneHandle graphene = GrapheneCore.handle(MyModClient.class);

String classpathHttpUrl = graphene.httpAssets().asset("web/index.html");
String mountedHttpUrl = graphene.httpUrl("web/index.html");
```

Important behavior:

- `graphene.httpAssets().asset(...)` and `graphene.httpUrl(...)` throw `IllegalStateException` when HTTP server is not running.
- `graphene.httpAssets().asset(...)` always targets shared classpath assets under `/assets/<mod-id>/...`.
- `graphene.httpUrl(...)` targets the consumer mount under `/mods/<mod-id>/...`.
- Mounted HTTP request resolution order is:
  1. filesystem (`fileRoot/request-path`)
  2. classpath fallback under `assets/<mod-id>/<request-path>`
  3. optional SPA fallback
- Requests under `/mods/<mod-id>/assets/...` are classpath-only and do not consult `fileRoot`.

## Recommendations

- Keep web assets under `assets/<mod-id>/web/...`.
- Use handle or `GrapheneClasspathUrls` helpers instead of hard-coded string concatenation.
- Keep filenames lowercase and extension-explicit.
- Prefer `grapheneHandle.httpUrl(...)` for consumer-mounted content and `grapheneHandle.httpAssets().asset(...)` for shared classpath assets.

---

Next: [Lifecycle](lifecycle.md)
