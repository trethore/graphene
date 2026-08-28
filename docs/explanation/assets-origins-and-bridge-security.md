# Assets, Origins, and Bridge Security

An embedded browser can display packaged, local, and remote content. Graphene treats a document's source and origin as
security boundaries because the JavaScript bridge can expose mod behavior to that document.

## Asset sources

### App assets

`context.appAssets()` produces URLs such as `app://assets/<namespace>/...`. Files are loaded from classpath resources
below `assets/<namespace>/...`.

Use app assets for normal packaged interfaces. A context defaults the namespace to its consumer mod ID, but the
namespace is a path component. Graphene's bridge-origin checks treat generated app asset URLs as the same
`app://assets` origin, and the default bridge policy allows them.

### Classpath assets

`context.classpathAssets()` produces `classpath://` URLs for resources below `assets/<namespace>/...`. Classpath assets
also share one Graphene-managed trust domain and are allowed by the default bridge policy.

### HTTP assets

When enabled, Graphene's loopback HTTP server exposes packaged resources and optional filesystem overrides. Every
document on that shared server origin is treated as Graphene-managed, including other consumers' mounts.

The server is useful for frontend development, browser APIs that expect HTTP, and SPA routing. It is not a general
public web server: Graphene requires its bind host to resolve to loopback.

### Remote documents

A browser can navigate to ordinary `http://` or `https://` pages according to its navigation policy. Remote content is
not Graphene-owned and does not receive the bridge under the default policy.

## Origins and navigation

Browsers use origins to separate documents by scheme, host, and port. Navigating a session can therefore change which
code controls the page and whether Graphene exposes privileged integrations.

`BrowserBridgePolicy` evaluates each main-frame document. The default Graphene-managed classification is broader than
consumer ownership: it covers all valid app and classpath asset namespaces and the shared Graphene HTTP origin.
Available policies include:

- Graphene-owned documents only.
- No documents.
- The browser's initial exact origin.
- An explicit set of exact normalized origins.

Wildcards are intentionally not supported by the origin allow-list helper.

## Bridge capability

The bridge supports arbitrary consumer-defined events and asynchronous request handlers. A handler can call mod logic,
read state, or trigger actions. Exposing those handlers to untrusted content creates a direct capability boundary
between web code and the mod.

Follow these rules:

- Keep the default bridge policy for packaged interfaces.
- Allow remote origins only when you control and trust their content and delivery.
- Validate every payload even when using typed JSON helpers.
- Give channels consumer-specific names.
- Never use the reserved `graphene:` prefix.
- Remove handlers that should no longer be reachable.
- Treat navigation as a replacement of the page-side JavaScript environment.
- Treat every Graphene-managed namespace and HTTP mount as trusted when using the default policy. If that is too broad,
  install a narrower policy and keep privileged handlers off shared pages.

## Other restricted capabilities

Graphene uses restrictive defaults beyond the bridge:

- Browser file access and browser file-selection dialogs are denied process-wide.
- Downloads are cancelled.
- Context menus are disabled.
- New browsing contexts are cancelled.
- File and JavaScript dialogs require platform or consumer presentation.

These features can cross from browser content into the operating system or Minecraft UI. Enable only the behavior
required by the interface and keep policy callbacks fast and non-blocking.

## Development settings

Remote debugging, filesystem asset overrides, and browser file access are useful during development but increase access
to browser internals or local files. Keep development configuration separate from release configuration.

## Related documentation

- [Manage assets and frontend development](../how-to/manage-assets-and-frontend-development.md)
- [Configure browser policies](../how-to/configure-browser-policies.md)
- [JavaScript bridge API](../reference/javascript-bridge-api.md)
