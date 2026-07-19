# Configure Browser Policies

`BrowserOptions` applies behavior and security policies when a browser is created. Defaults favor packaged Graphene
content and reject capabilities that require an explicit consumer decision.

## Build browser options

```java
BrowserOptions options =
        BrowserOptions.builder()
                .maximumFrameRate(30)
                .transparent(true)
                .contextMenuPolicy(BrowserContextMenuPolicy.standard())
                .build();
```

Pass the options to a surface:

```java
BrowserSurface surface =
        BrowserSurface.builder(context)
                .url(url)
                .options(options)
                .size(width, height)
                .build();
```

## Restrict bridge exposure

The default policy allows the bridge only for Graphene-owned app, classpath, and built-in HTTP documents.

Disable it completely:

```java
.bridgePolicy(BrowserBridgePolicy.disabled())
```

Allow only the origin used to create the browser:

```java
.bridgePolicy(BrowserBridgePolicy.initialOrigin())
```

Allow an exact origin:

```java
BrowserBridgeOrigin origin = new BrowserBridgeOrigin("https", "example.com", 443);

BrowserOptions options =
        BrowserOptions.builder()
                .bridgePolicy(BrowserBridgePolicy.allowOrigins(origin))
                .build();
```

Bridge policies must be thread-safe and non-blocking. An exception or `null` decision denies exposure. Do not expose
privileged Java handlers to remote content you do not control.

## Control navigation and popups

The default navigation policy keeps ordinary navigation in the current session and cancels new browsing contexts.

Open selected links in the system browser:

```java
BrowserNavigationPolicy policy =
    request -> {
      if (request.type() == BrowserNavigationPolicy.Type.POPUP) {
        return BrowserNavigationPolicy.Decision.EXTERNAL_BROWSER;
      }
      return BrowserNavigationPolicy.Decision.SAME_SESSION;
    };
```

Other decisions cancel navigation or leave follow-up work to the consumer. Navigation policies run synchronously on the
browser callback thread and must not block.

## Enable context menus

Context menus are disabled by default. Enable Graphene's standard safe command set:

```java
.contextMenuPolicy(BrowserContextMenuPolicy.standard())
```

The Fabric integration provides its platform presenter when no custom presenter is supplied. Install
`contextMenuPresenter(...)` only when your UI needs a different asynchronous presentation.

Custom policies receive browser-proposed items and return the subset to present. Preserve command IDs from the proposed
items so the selected action can be executed correctly.

## Handle downloads

Downloads are cancelled by default. Save directly to an explicit path:

```java
.downloadPolicy(request ->
    BrowserDownloadPolicy.Decision.saveTo(
        downloadDirectory.resolve(request.suggestedFileName())))
```

Use Chromium's native save dialog:

```java
.downloadPolicy(request -> BrowserDownloadPolicy.Decision.showSaveDialog())
```

The download policy runs synchronously on the browser callback thread and must not block. Validate consumer-selected
paths and avoid deriving unrestricted filesystem paths from remote filenames.

## Present file and JavaScript dialogs

Graphene supports asynchronous custom presenters:

```java
.fileDialogPresenter(request -> showFilePicker(request))
.jsDialogPresenter(request -> showBrowserDialog(request))
```

A file presenter completes with selected paths or an empty list to cancel. A JavaScript-dialog presenter completes with
`BrowserJsDialogPresenter.Result.accept(...)` or `Result.cancel()`.

When no custom presenter is configured, the Fabric platform integration supplies its default presenter.

## Allow direct file access only when required

Browser file access is a process-wide setting and is denied by default:

```java
GrapheneGlobalConfig global =
        GrapheneGlobalConfig.builder().allowBrowserFileAccess().build();
```

Only enable it for content that must directly load local `file://` resources. Prefer app or loopback HTTP assets, which
provide narrower access.

## Next steps

- [Review all defaults](../reference/configuration-and-defaults.md).
- [Understand bridge and origin security](../explanation/assets-origins-and-bridge-security.md).
- [Observe downloads and browser events](control-and-observe-the-browser.md).
