# Troubleshoot Graphene

Use the symptom -> cause -> fix entries below before inspecting internal code.

## Registration fails

### `Graphene platform services are not installed`

**Cause:** Graphene was called before its Fabric bootstrap ran, or Graphene is missing from the runtime mod set.

**Fix:** declare a `grapheneui` dependency in `fabric.mod.json`, include the Graphene mod dependency, and register from
a Fabric client initializer.

### `No loaded mod with id ... is available`

**Cause:** the ID passed to `Graphene.register(String)` does not match a loaded Fabric mod.

**Fix:** use the exact `id` from `fabric.mod.json`, or register with an anchor class from your mod.

### `Graphene consumer registration is closed`

**Cause:** registration happened after Graphene began platform startup.

**Fix:** register once during client initialization, not when opening the first screen.

### The consumer is already registered with different configuration

**Cause:** the same mod ID called `register` more than once with unequal configurations.

**Fix:** create one configuration in the client initializer and retain the returned `GrapheneContext`.

## Browser creation fails

### `BrowserRuntimeUnavailableException`

**Cause:** the runtime is `NEW`, `STARTING`, `FAILED`, `STOPPING`, or `STOPPED` rather than `RUNNING`.

**Fix:** inspect `context.runtime().state()`, observe `context.runtime().initialization()`, and create early browser
sessions only after successful initialization.

### Runtime initialization failed

**Cause:** JCEF installation, native startup, HTTP startup, or global configuration failed.

**Fix:** inspect the initialization exception and Graphene browser log. The default runtime is installed under:

```text
./graphene/browser-runtime/<jcef-version>/<platform>/
```

Its Chromium/JCEF log is:

```text
./graphene/browser-runtime/<jcef-version>/<platform>/logs.txt
```

The base path changes when `browserRuntimePath` is configured.

## The page is blank

**Cause:** the URL points to a missing resource, the browser has not painted its first frame, or a load failed.

**Fix:**

1. Subscribe to `browser.onLoad(...)` and `browser.onConsoleMessage(...)`.
2. Confirm the resource is under `assets/<mod-id>/...`.
3. Confirm the path passed to `appAssets().url(...)` does not repeat the namespace.
4. Open DevTools and inspect the Network and Console panels.

## The JavaScript bridge is missing

**Cause:** the current main-frame document was denied by `BrowserBridgePolicy`, JavaScript is disabled, or code ran
before the injected bridge was available.

**Fix:**

1. Start with a URL from `context.appAssets()`, `context.classpathAssets()`, or Graphene's HTTP assets.
2. Keep the default `grapheneOwnedDocuments()` policy unless another origin is required.
3. Call `await globalThis.grapheneBridge.ready()` before sending messages.
4. Reinstall JavaScript handlers after navigation.

Do not solve this by allowing every remote origin.

## HTTP assets fail to start or load

**Cause:** HTTP hosting is disabled, a fixed port is occupied, the bind host is not loopback, or consumers contributed
conflicting host/port settings.

**Fix:** enable `GrapheneHttpConfig`, use the default random port range, keep the host on loopback, and make shared
server settings identical across consumers.

For `fileRoot`, confirm that the resolved file remains inside the configured root and is readable. Filesystem resources
take precedence over packaged resources.

## Global configuration conflicts

**Cause:** consumers contributed different browser runtime paths, remote-debug settings, or browser-file-access
policies.

**Fix:** inspect `GrapheneGlobalConfigConflictException.setting()` and `contributions()`. Align the conflicting
process-wide values. Extension folders are combined rather than required to match.

## Mouse or keyboard input does not work

**Cause:** a custom surface is not focused, input is not forwarded, or rendered bounds differ from the bounds supplied
to `BrowserSurfaceInputAdapter`.

**Fix:** use `GrapheneWebViewWidget` for normal screens. For custom surfaces, forward focus, pointer, scroll, key, and
text input with the real rendered position and dimensions.

## Rendering is blurry or incorrectly scaled

**Cause:** browser resolution is too low for the rendered size, or logical size was confused with browser pixel
resolution.

**Fix:** use automatic resolution with the current GUI scale, or set a fixed resolution appropriate for the displayed
size. Avoid rendering a small browser viewport across a much larger area.

## DevTools cannot find the browser

**Cause:** remote debugging is disabled, the runtime is not running, or multiple targets share the session's URL and
title.

**Fix:** enable remote debugging before startup, wait for initialization, and give pages distinct titles or URLs.
Inspect the specific DevTools exception rather than treating every failure as disabled debugging.

## A browser remains alive after closing a screen

**Cause:** screen auto-close was disabled, a custom surface was not closed, or a persistent owner retained the widget.

**Fix:** close the highest-level object you own and clear its field. Also unsubscribe bridge and browser listeners owned
by the same component.

## Next steps

- [Review browser lifecycle](manage-browser-lifecycle.md).
- [Enable DevTools](use-devtools.md).
- [Look up configuration defaults](../reference/configuration-and-defaults.md).
