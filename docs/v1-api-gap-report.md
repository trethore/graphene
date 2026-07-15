# V1 Public API Gap Report

Date: 2026-07-14

Reviewed state:

- Graphene `migrate` at `5cf9c48`;
- dialog presenter work from `8dc1ff5`;
- JCEF `146.0.10-jcefgithub.5` and the checked-in JCEF/CEF source references;
- the previous `main` and `dev` Graphene APIs, used only to identify regressions and previously useful features.

## Executive summary

The new API has the right architectural direction: common browser contracts contain no JCEF or Minecraft types,
browser sessions can exist independently from Fabric surfaces, and dialog presentation is now customizable per browser.

The main V1 risk is not a lack of wrappers for every JCEF feature. It is freezing contracts that are currently
non-functional, incorrectly scoped, security-sensitive, or accidentally public. The highest-priority work is:

1. make every advertised option effective or remove it;
2. hide the backend/bootstrap SPI from consumer API;
3. define safe navigation, popup, bridge-origin, and download policies;
4. complete observable browser state, especially title and console events;
5. make runtime ownership safe for a multi-mod process;
6. finish the normalized input and frame contracts before they become difficult to change;
7. document lifecycle, threading, nullability, and compatibility guarantees.

The dialog presenter commit closes the per-session JavaScript and file-dialog customization gap. It does not yet
provide equivalent policy/presenter hooks for downloads, popups, context menus, authentication, certificate errors,
or global platform presentation.

## Current coverage

### Public API already in good shape

- consumer registration through `Graphene` and `GrapheneContext`;
- implementation-neutral asset URLs;
- browser lifecycle, navigation, history, focus, resizing, normalized low-level input, and closure;
- load events represented by Graphene-owned values;
- Java/JavaScript events and request/response bridge;
- CPU frame snapshots and Fabric rendering surfaces;
- runtime state, HTTP server status, and remote-debugging port discovery;
- per-browser JavaScript and file-dialog presenters;
- Fabric widget, input adapter, and screen auto-close utilities.

### JCEF integration currently installed

The shared `CefClient` installs handlers for:

- load events;
- context menus, currently always suppressed;
- popups and new-tab navigation, currently redirected into the same browser;
- downloads, currently saved automatically;
- file dialogs;
- JavaScript dialogs;
- the Graphene message router.

Rendering, cursor changes, and drag source/target behavior are implemented directly by
`GrapheneCefBrowserSession`.

JCEF also provides display, focus, keyboard, print, request/resource, authentication, certificate, window, cookie,
zoom, find, source/text, screenshot, download control, and DevTools capabilities that Graphene does not currently
model publicly. Most of these should not be exposed one-for-one.

## Release blockers

### 1. Advertised options that do not work

- [x] Resolved for V1
- [x] Apply `backgroundColor` with `transparent` controlling alpha.
- [x] Apply `javascriptEnabled` per browser.
- [x] Restrict the built-in asset server to HTTP.
- [x] Pass file filters to the default Fabric file-dialog presenter.

`BrowserOptions.backgroundColor` is a 24-bit RGB value. Browser sessions default to transparent with a white RGB
background. The `transparent` option controls whether the effective background alpha is fully transparent or fully
opaque. Opaque browser backgrounds and JavaScript disablement are applied per browser before navigating to the
requested initial URL.

The built-in asset server is HTTP-only. `GrapheneHttpConfig` does not expose a configurable URL scheme until TLS and
its certificate lifecycle have a defined API.

The default Fabric file-dialog presenter flattens CEF's filter groups into TinyFD's single filter group, preserving
expanded extension patterns and using a generic description when multiple descriptions are present.

### 2. Backend SPI is accidentally consumer-facing

- [x] Resolved for V1

Backend installation is no longer part of the consumer API. `GrapheneBackend` and
`GrapheneBackendRegistry` were removed, and the public `Graphene` facade now delegates directly to the internal
runtime controller.

The dependency-injection constructor of `GrapheneContext` is package-private. Consumers obtain contexts through
`Graphene.register(...)` and can no longer install a replacement backend or manually construct a context.

### 3. Shared runtime lifecycle is unsafe as a consumer API

- [x] Resolved for V1

`GrapheneRuntime` is now a read-only consumer view exposing state, initialization completion or failure, remote
debugging information, and HTTP status. Initialization, registration closure, and shutdown are owned exclusively by
the internal platform lifecycle controller.

The public view is a separate object from `GrapheneRuntimeController`, preventing consumers from casting the returned
runtime back to the lifecycle controller. Its initialization stage preserves the original startup failure for both
early and late observers and does not expose the controller's mutable completion future. HTTP status is exposed
through a read-only view rather than the closeable internal server runtime.

### 4. Browser creation readiness is undefined

- [x] Resolved for V1

`BrowserSessions.create` remains synchronous and succeeds only while the process-wide runtime is `RUNNING`. Creation
in every other runtime state fails with `BrowserRuntimeUnavailableException`, which exposes the observed
`GrapheneRuntimeState`. A failure observed in `FAILED` preserves the original initialization failure as its cause.

Consumers that may create browsers before startup completes can await `GrapheneRuntime.initialization()` and marshal
their continuation to the required platform thread. The initialization-stage lifecycle and callback-thread contract
are documented. Creation and shutdown are serialized by the runtime controller so readiness races cannot reach an
uninitialized or cleared JCEF client.

### 5. Navigation and popup behavior needs a public policy

- [x] Resolved for V1

JCEF distinguishes normal navigation, `window.open`/popups, and URLs opened from a tab. Graphene previously handled
the latter two by loading the target URL into the same browser and canceling the new window. This silently destroyed
the current page and gave consumers no way to:

- reject the navigation;
- open it in an external system browser;
- create another `BrowserSession`;
- preserve target-frame information;
- allow only trusted origins.

`BrowserNavigationPolicy` is configured per browser through `BrowserOptions`. It synchronously receives Graphene-owned
snapshots containing the session, request type, target URL, source frame, target frame name, user-gesture and redirect
flags, and a stable requested-disposition enum. Decisions support cancellation, navigation in the same session,
opening in the external system browser, and consumer-managed handling.

The default policy preserves ordinary main-frame and current-tab navigation while canceling popups and other new-tab
or new-window requests. This avoids silently replacing the current page. `CONSUMER_MANAGED` cancels Graphene's default
handling after the policy callback has arranged any required follow-up work, such as scheduling creation of another
session.

Main-frame navigation is filtered through the same policy. Policy failures and null decisions fail closed. Policy
callbacks run synchronously on the browser callback thread and must not block; same-session and external-browser
actions are marshaled to the platform thread when the original navigation cannot be preserved.

### 6. Bridge origin policy is missing

- [x] Resolved for V1

`BrowserBridgePolicy` is configured per browser through `BrowserOptions`. The default policy exposes the bridge only
to main-frame documents loaded from Graphene-owned app, classpath, or built-in HTTP URLs. Policies are also provided
for disabling the bridge, following the requested initial origin, and allowing an exact set of origins. Custom policy
failures and null decisions fail closed.

Bridge authorization is enforced both before bootstrap injection and for every inbound CEF query using the requesting
frame URL. Subframes are denied for V1, preventing an untrusted iframe from bypassing bootstrap controls by invoking
CEF's query function directly. Navigation immediately resets bridge readiness, denied documents clear queued outbound
messages, and stale ready handshakes cannot reactivate a later document.

The `graphene:` channel namespace is reserved for Graphene integrations. Consumer bridge methods reject reserved
channels, while internal clipboard, mouse, and file-dialog routing use a separate internal access path.

### 7. Downloads need a public decision and observation API

- [ ] Resolved for V1

The current download handler automatically writes files to `~/Downloads/grapheneui`, silently falls back to the temp
directory, and exposes no progress or cancellation API. JCEF provides all required primitives: suggested path,
metadata, progress, pause, resume, and cancel.

For V1, add:

- a `BrowserDownloadPresenter` or decision handler for accept/cancel and target path;
- immutable `BrowserDownload` snapshots/events;
- a control handle for cancel, and optionally pause/resume;
- explicit failure/completion states;
- a configurable default download directory if automatic downloads remain supported.

The safe default should not silently download arbitrary remote content without a consumer-visible policy.

### 8. Core observable browser state is incomplete

- [ ] Resolved for V1

The previous implementation exposed title state and title listeners. The migration does not install a JCEF display
handler and `BrowserSession` has no title API. This is a direct regression for screens that display page titles.

V1 should add:

- `currentTitle()`;
- title-change subscription;
- address-change subscription or a clearly defined relationship between `currentUrl()` and load events;
- console-message events with severity, message, source, and line.

Console messages are especially useful because JavaScript execution is otherwise fire-and-forget and embedded UI
failures are difficult to diagnose.

Use a common subscription type for listeners. `GrapheneBridgeSubscription` should be renamed/generalized, or a new
`GrapheneSubscription` should be shared by load, title, frame, download, and console listeners.

### 9. Load event types are not yet implementation-neutral enough

- [ ] Resolved for V1

`BrowserLoadStarted.navigationType` is a raw JCEF enum name stored as `String`. `BrowserLoadFailed` exposes a raw
integer and JCEF enum name. This contradicts the API-boundary plan to use Graphene-owned navigation and error types.

Add stable enums/value types before V1, with an `UNKNOWN` value and optional raw diagnostic code where useful. Also
consider removing `browserId` from session-scoped listener events, or expose a stable Graphene session identifier. The
current integer is a JCEF implementation detail and is redundant when listeners are registered on a session.

### 10. Frame contract is incomplete for custom renderers

- [ ] Resolved for V1

`BrowserFrame` exposes a byte buffer without declaring its public pixel contract. Custom renderers need stable answers
for:

- channel order;
- alpha premultiplication;
- row order and stride;
- color space;
- whether dirty regions apply to the returned full-frame snapshot.

`latestFrame()` also returns `null` before the first paint without annotation or documentation, and there is no frame
availability subscription. Polling is acceptable for the Fabric renderer but weak for headless/custom integrations.

Before V1, define the pixel format explicitly and either return `Optional<BrowserFrame>`, annotate nullability, or
provide a non-null frame state abstraction. Add a frame listener/subscription with documented coalescing and
backpressure semantics.

### 11. Normalized input still leaks platform/JCEF conventions

- [ ] Resolved for V1

The API-boundary document planned a `BrowserKey` abstraction, but the current API only exposes integer `keyCode`,
`nativeKeyCode`, and `scanCode`. Custom integrations must understand Windows virtual-key codes and platform-native scan
code packing to produce correct input.

V1 should add a stable `BrowserKey` enum/value while retaining optional raw metadata for unknown keys and unusual
layouts.

`BrowserTextInput` stores one Java `char`, which does not represent a Unicode code point. Change it to a code point or
string before the record is frozen.

Extra mouse buttons are handled by Fabric through private bridge events instead of the common input contract. Decide
whether V1 supports them as public browser input and expose that consistently if it does.

## Strong V1 candidates

These are useful and supported by the current JCEF binding, but can be deferred if V1 explicitly defines a narrower
scope.

### Context-menu presenter or policy

Graphene currently clears every context menu. Add a presenter/policy if consumers should support link actions,
selection copy, editable controls, media actions, or custom menu entries. Otherwise document that context menus are
disabled in V1.

### DevTools discovery

Remote debugging can be enabled and its port queried, but there is no API to resolve the inspected page endpoint. The
previous implementation exposed DevTools URI resolution/opening. The in-process JCEF DevTools browser is explicitly
unimplemented, so V1 should either:

- expose only remote DevTools target discovery as Graphene-owned URIs; or
- remove any implication that `openDevTools` is supported.

### Zoom, find, screenshot, and source/text extraction

JCEF already supports these operations. Suggested V1 additions, in descending value:

- zoom get/set/reset;
- find/stop finding;
- screenshot as a Graphene image/frame result;
- asynchronous page source or text retrieval.

The implementation already overrides JCEF screenshot creation internally, but `BrowserSession` does not expose it.

### Profile, storage, and cookies

All sessions currently use the global request context and shared cache. V1 should at least document this. If isolation
is a product goal, add a Graphene-owned profile/request-context option covering:

- shared versus isolated storage;
- persistent versus in-memory cache;
- session cookie persistence;
- cookie read/write/delete operations.

This is important for multiple mods using unrelated remote services in the same Minecraft process.

### Authentication and certificate decisions

JCEF exposes HTTP authentication and certificate-error callbacks. Add Graphene-owned presenters/policies if remote web
applications, proxies, or development servers with custom certificates are in V1 scope. Keep certificate errors
denied by default.

### Fullscreen, tooltip, and status events

The JCEF display handler reports fullscreen requests, tooltips, and status messages. Fullscreen in particular needs an
explicit policy in an embedded Minecraft surface. These can be omitted if V1 documents that they are unsupported.

## Recommended post-V1 scope

Do not block V1 on these unless a concrete consumer requires them:

- printing and PDF printing;
- arbitrary request/response interception;
- custom resource handlers beyond Graphene's asset systems;
- raw cookie filtering;
- arbitrary CEF preferences or command-line switches;
- accelerated/shared-texture rendering;
- external begin-frame scheduling;
- accessibility and audio handlers not surfaced by the current JCEF Java binding;
- generic JCEF handler injection.

A generic escape hatch would violate the established implementation boundary and make backend replacement and binary
compatibility substantially harder.

## API contract and release hygiene

Only a small fraction of the public types currently have Javadoc. Before V1, document:

- callback thread for every listener and presenter;
- whether presenter stages may complete on any thread;
- close idempotency and behavior of methods after close;
- ownership when a `BrowserSurface` closes its `BrowserSession`;
- whether an existing session can be attached to a surface;
- nullability, especially `latestFrame()`;
- listener ordering and exception isolation;
- bridge queueing across navigation;
- reserved bridge channels;
- global configuration merge rules between mods;
- security implications of file access, extensions, remote debugging, and remote navigation.

The current global file-access merge is especially important: one consumer selecting `ALLOW` enables the global CEF
preference for all sessions. This should use conflict detection, a host-only setting, or a clearly documented
process-wide trust model instead of implicit allow-wins behavior.

Add an API compatibility check such as Revapi or japicmp before publishing the first stable artifact. Define which
packages are supported API and exclude internal/platform implementation packages from compatibility promises.

## Suggested implementation order

1. Move backend installation and context construction out of consumer API.
2. Restrict public runtime lifecycle control and define browser creation readiness.
3. Add bridge-origin and navigation/popup policies.
4. Add title and console state/events.
5. Add download decision, progress, and control APIs.
6. Replace raw load-event strings with Graphene-owned types.
7. Finalize frame format/nullability/subscriptions and normalized key/text input.
8. Decide and document context-menu, DevTools, profile/cookie, auth, and fullscreen scope.
9. Complete Javadoc, usage guides, API compatibility checks, and migration notes.

## Proposed V1 acceptance checklist

- [x] Every public option has tested observable behavior.
- [x] No consumer API exposes JCEF, jcefgithub, internal classes, or backend installation details.
- [x] One mod cannot initialize or shut down the process-global runtime for all mods.
- [x] Session creation before/after runtime transitions has deterministic behavior.
- [x] Untrusted navigation cannot inherit bridge access by accident.
- [ ] Popups, external URLs, and downloads have explicit policies.
- [ ] Title, URL, loading, and console state can be observed without JCEF types.
- [ ] Load events use stable Graphene-owned enums/value types.
- [ ] Frame pixel layout and pre-first-frame behavior are documented and tested.
- [ ] Key and text input support stable keys and full Unicode semantics.
- [ ] Listener/presenter threading, lifecycle, nullability, and ownership are documented.
- [ ] Global configuration conflicts and security-sensitive merge behavior are defined.
- [ ] Public API compatibility is checked in CI.
