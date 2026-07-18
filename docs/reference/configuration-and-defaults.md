# Configuration and Defaults

This page summarizes the settings that affect Graphene consumers, the shared runtime, and individual browsers.

## Configuration scopes

| Type                      | Scope                     | Resolution                                      |
|---------------------------|---------------------------|-------------------------------------------------|
| `GrapheneConfig`          | One registered consumer   | Contains its container and global contribution. |
| `GrapheneContainerConfig` | One registered consumer   | Independent for each consumer.                  |
| `GrapheneGlobalConfig`    | Process-wide contribution | Resolved across every registered consumer.      |
| `BrowserOptions`          | One browser session       | Fixed when the browser is created.              |

## `GrapheneContainerConfig`

| Setting | Default  | Meaning                                                                  |
|---------|----------|--------------------------------------------------------------------------|
| `http`  | Disabled | Enables this consumer's mount on Graphene's shared loopback HTTP server. |

Use `http(...)` to enable the mount and `disableHttp()` to remove it from a builder.

## `GrapheneHttpConfig`

| Setting       | Default                                            | Meaning                                                        |
|---------------|----------------------------------------------------|----------------------------------------------------------------|
| `bindHost`    | `127.0.0.1`                                        | Loopback host used by the shared HTTP server.                  |
| Port          | Random available port in `20000`-`21000` inclusive | Shared server port selection.                                  |
| `fileRoot`    | None                                               | Filesystem root whose files override packaged consumer assets. |
| `spaFallback` | None                                               | Asset returned when a requested consumer resource is missing.  |

Port values must be between `1024` and `65535`. All HTTP-enabled consumers must agree on bind host and port selection.
`fileRoot` and SPA fallback belong to each consumer mount.

## `GrapheneGlobalConfig`

| Setting              | Default                                  | Resolution across consumers              |
|----------------------|------------------------------------------|------------------------------------------|
| Browser runtime path | `./graphene/browser-runtime`             | Explicit contributions must match.       |
| Extension folders    | Empty                                    | Folders from all consumers are combined. |
| Remote debugging     | Disabled when no contribution enables it | Explicit contributions must match.       |
| Browser file access  | `DENY`                                   | Contributions must match.                |

The effective runtime path adds JCEF version and platform directories below the configured base path.

`GrapheneGlobalConfigConflictException` reports the exclusive setting and each conflicting consumer contribution.

## `GrapheneRemoteDebugConfig`

Calling `GrapheneRemoteDebugConfig.builder()` starts with remote debugging enabled and a random available port.

| Setting         | Builder default       | Meaning                                                 |
|-----------------|-----------------------|---------------------------------------------------------|
| `enabled`       | `true`                | Enables Chromium remote debugging.                      |
| Port            | Random available port | Select with `port(...)` or restore with `randomPort()`. |
| Allowed origins | Chromium/JCEF default | Passed to Chromium when explicitly configured.          |

`GrapheneRemoteDebugConfig.disabled()` and `builder().disable()` create an explicit disabled contribution.
`GrapheneGlobalConfig` itself has no remote-debug contribution by default, so the runtime remains disabled unless a
consumer enables it.

## `BrowserOptions`

| Setting                     | Default                          | Constraints or behavior                                         |
|-----------------------------|----------------------------------|-----------------------------------------------------------------|
| Maximum frame rate          | `60`                             | Integer from `1` through `60`.                                  |
| Transparent                 | `true`                           | Browser frames preserve transparency.                           |
| Background color            | `0xFFFFFF`                       | 24-bit RGB value used for opaque rendering.                     |
| JavaScript enabled          | `true`                           | Disable only for noninteractive documents.                      |
| Bridge policy               | Graphene-owned documents         | App, classpath, and built-in HTTP documents receive the bridge. |
| Navigation policy           | Same-session ordinary navigation | New browsing contexts are cancelled.                            |
| Download policy             | Cancel                           | No download starts without an explicit policy.                  |
| Context-menu policy         | Disabled                         | No menu is presented.                                           |
| Context-menu presenter      | Platform default                 | Used when the policy returns menu items.                        |
| File-dialog presenter       | Platform default                 | Handles browser file selection.                                 |
| JavaScript-dialog presenter | Platform default                 | Handles alert, confirm, prompt, and before-unload.              |

## Policy failure behavior

| Policy                     | Callback requirements                                         | Exception or `null` result |
|----------------------------|---------------------------------------------------------------|----------------------------|
| `BrowserBridgePolicy`      | Thread-safe and non-blocking                                  | Denies bridge exposure.    |
| `BrowserNavigationPolicy`  | Runs synchronously on browser callback thread; must not block | Cancels navigation.        |
| `BrowserDownloadPolicy`    | Runs synchronously on browser callback thread; must not block | Cancels download.          |
| `BrowserContextMenuPolicy` | Return browser-proposed items to present                      | Disables the menu.         |

Failed or `null` asynchronous presenter completions are treated as cancellation or rejection as documented by each
presenter interface.

## Runtime defaults

| Behavior               | Default                                                             |
|------------------------|---------------------------------------------------------------------|
| Runtime starts         | Only when at least one consumer registered before platform startup. |
| HTTP server starts     | Only when at least one consumer enables HTTP.                       |
| Browser cache          | Stored below the resolved JCEF runtime directory.                   |
| JCEF log severity      | Warning.                                                            |
| Off-screen rendering   | Enabled.                                                            |
| Chromium color profile | sRGB.                                                               |

## Related documentation

- [Compatibility and installation](compatibility-and-installation.md)
- [Architecture and runtime](../explanation/architecture-and-runtime.md)
- [Configure browser policies](../how-to/configure-browser-policies.md)
