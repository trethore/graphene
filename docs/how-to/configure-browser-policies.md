# Configure Browser Policies

`BrowserOptions` applies behavior and security policies when a browser is created. Defaults favor Graphene-managed
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

Pass the options to a view:

```java
BrowserView view =
        BrowserView.builder(context)
                .url(url)
                .options(options)
                .resolution(width, height)
                .build();
```

## Restrict bridge exposure

The default policy allows the bridge only for Graphene-managed app, classpath, and shared HTTP-origin documents. These
sources are shared trust domains: an asset namespace or consumer HTTP mount is not a separate browser origin.

Disable it completely:

```java
.bridgePolicy(BrowserBridgePolicy.disabled())
```

Allow only the origin used to create the browser:

```java
.bridgePolicy(BrowserBridgePolicy.initialOrigin())
```

Origin-only policies do not distinguish app asset namespaces or consumer mounts on Graphene's shared HTTP server. Use a
custom policy that inspects `request.documentUrl()` when those paths must be isolated.

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
items so the selected action can be executed correctly. The policy runs synchronously on the browser callback thread and
must not block.

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
paths, create required parent directories before returning `saveTo(...)`, and avoid deriving unrestricted filesystem
paths from remote filenames.

## Allow browser file access only when required

Browser file access is a process-wide setting and is denied by default:

```java
GrapheneGlobalConfig global =
        GrapheneGlobalConfig.builder().allowBrowserFileAccess().build();
```

`ALLOW` is required for browser file-selection requests, including file inputs and file-system picker APIs, as well as
direct local-file capabilities. With the default `DENY` policy, Graphene cancels file-dialog requests before invoking a
presenter.

Enable it only when required by trusted content. Prefer app or loopback HTTP assets over direct `file://` resources, and
remember that all consumers must contribute the same process-wide file-access policy.

Use the per-browser file-dialog policy to restrict which documents may present a picker:

```java
.fileDialogPolicy(request ->
        request.documentUrl().startsWith("app://")
                ? BrowserFileDialogPolicy.Decision.ALLOW
                : BrowserFileDialogPolicy.Decision.DENY)
```

The default file-dialog policy allows requests after the process-wide policy has allowed browser file access. Policy
exceptions and `null` decisions cancel the request. The request source distinguishes routed `showDirectoryPicker()`
calls from other browser dialogs.

`showDirectoryPicker()` is routed through a restricted internal message that does not expose the public Graphene
bridge. If routing cannot be established, Graphene rejects the JavaScript promise instead of falling back to a file
picker. Folder-upload inputs remain unsupported because Chromium presents an additional confirmation UI that JCEF does
not expose safely for off-screen rendering.

The File System Access API is exposed only in secure contexts. Use a remote HTTPS page or enable Graphene's loopback
HTTP asset mount and load the page through `context.httpUrl(...)`. Do not assume that Chromium will expose the API to
`app://`, `classpath://`, or `file://` documents; feature-detect `window.showDirectoryPicker` before using it.

## Present file and JavaScript dialogs

Graphene supports asynchronous custom presenters:

```java
.fileDialogPresenter(request -> showFilePicker(request))
.jsDialogPresenter(request -> showBrowserDialog(request))
```

A file presenter completes with selected paths or an empty list to cancel. It is called only when the process-wide
browser file-access policy is `ALLOW`. A JavaScript-dialog presenter completes with
`BrowserJsDialogPresenter.Result.accept(...)` or `Result.cancel()` and does not require file access.

When no custom presenter is configured, the Fabric platform integration supplies its default presenter.

## Next steps

- [Review all defaults](../reference/configuration-and-defaults.md).
- [Understand bridge and origin security](../explanation/assets-origins-and-bridge-security.md).
- [Observe downloads and browser events](control-and-observe-the-browser.md).
