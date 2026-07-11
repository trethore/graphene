# Graphene Migration Plan

Date: 2026-07-11

## Task

Migrate the implementation in `references/trethore-graphene-main/` into the new multi-project architecture. The public
Java namespace will become `io.github.trethore.graphene`, while each Minecraft-version release will be a single Fabric
mod artifact containing both the common API and the version-specific implementation.

The project name remains Graphene. The stable Fabric mod ID and asset namespace will be `grapheneui` to avoid conflicts
with other Minecraft mods.

## Confirmed Decisions

1. Replace `tytoo.grapheneui` with `io.github.trethore.graphene`. No compatibility wrappers are planned.
2. Publish one release artifact per supported Minecraft version. The artifact combines `packages/common` with the
   relevant Minecraft/Fabric package.
3. Use `grapheneui` as the Fabric mod ID and resource namespace.
4. Do not expose JCEF types through the public API. JCEF is an implementation detail that may be replaced independently.
5. `packages/common` must not depend on Minecraft, Fabric, Loom, or a particular Minecraft version.
6. `packages/fabric-1.21.11` depends on common; common must never depend on Fabric.

## Target Architecture

```text
Consumer mod
    |
    v
graphene-<minecraft-version> release artifact
    |
    +-- Fabric/Minecraft API and implementation
    |
    +-- embedded common API and implementation
            |
            +-- internal JCEF adapter
```

### Common package layout

```text
io.github.trethore.graphene.api
io.github.trethore.graphene.api.bridge
io.github.trethore.graphene.api.browser
io.github.trethore.graphene.api.config
io.github.trethore.graphene.api.runtime
io.github.trethore.graphene.api.url
io.github.trethore.graphene.internal.bridge
io.github.trethore.graphene.internal.browser
io.github.trethore.graphene.internal.cef
io.github.trethore.graphene.internal.http
io.github.trethore.graphene.internal.platform
io.github.trethore.graphene.internal.resource
```

### Fabric package layout

```text
io.github.trethore.graphene.fabric.api.screen
io.github.trethore.graphene.fabric.api.surface
io.github.trethore.graphene.fabric.api.widget
io.github.trethore.graphene.fabric.internal.browser
io.github.trethore.graphene.fabric.internal.cef
io.github.trethore.graphene.fabric.internal.input
io.github.trethore.graphene.fabric.internal.platform
io.github.trethore.graphene.fabric.internal.screen
io.github.trethore.graphene.mixin
```

## Files to Modify

### Project configuration

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `packages/common/build.gradle.kts`
- `packages/fabric-1.21.11/build.gradle.kts`

### Common package

- `packages/common/src/main/java/io/github/trethore/graphene/api/**`
- `packages/common/src/main/java/io/github/trethore/graphene/internal/**`
- `packages/common/src/main/resources/assets/grapheneui/bridge/**`
- `packages/common/src/test/java/io/github/trethore/graphene/**`

### Fabric 1.21.11 package

- `packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/FabricBootstrap.java`
- `packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/fabric/api/**`
- `packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/fabric/internal/**`
- `packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/mixin/**`
- `packages/fabric-1.21.11/src/main/resources/fabric.mod.json`
- `packages/fabric-1.21.11/src/main/resources/grapheneui.mixins.json`
- `packages/fabric-1.21.11/src/main/resources/assets/grapheneui/**`
- `packages/fabric-1.21.11/src/test/java/io/github/trethore/graphene/**`

### Documentation

- `README.md`
- `docs/README.md`
- Additional migrated documents under `docs/`

Files under `references/` are read-only migration sources and must not be edited.

## Implementation Plan

### 1. Inventory and classify the old implementation

Create a migration matrix for every class and resource under `references/trethore-graphene-main/src/`.

Classify each item by responsibility rather than direct imports alone:

- Configuration, bridge protocol, URL handling, HTTP server, MIME handling, runtime state, and platform-neutral browser
  behavior -> common.
- Fabric lifecycle, mod discovery, Minecraft rendering, input conversion, cursors, screens, overlays, dialogs, widgets,
  mixins, and native window access -> Fabric.
- Classes combining both responsibilities -> split before migration.
- Debug client code and browser test pages -> Fabric debug source set.

Record old path, new path, dependencies, required refactoring, associated tests, and migration status.

### 2. Correct the Gradle dependency and artifact model

Update `packages/common/build.gradle.kts` to:

- remain a plain Java 21 library;
- depend internally on JCEF GitHub without exposing it as API;
- declare Gson and SLF4J directly where required;
- produce test and source outputs for development;
- avoid Loom, Minecraft, and Fabric dependencies.

Update `packages/fabric-1.21.11/build.gradle.kts` to:

- use `implementation(project(":packages:common"))` for compilation;
- use `include(project(":packages:common"))` to embed common in the versioned release JAR;
- package JCEF and its required runtime content exactly once;
- publish only the remapped Fabric artifact for Minecraft 1.21.11;
- use a version-specific archive name such as `graphene-1.21.11` while keeping mod ID `grapheneui`;
- ensure the source JAR contains both common and Fabric public sources where practical.

Validate the JCEF dependency and native layout against:

- `references/io.github.trethore-jcefgithub-146.0.10.3/`
- `references/trethore-jcef-master/`
- `references/chromiumembedded-cef-master/`

### 3. Define strict API boundaries

Create a public API that exposes Graphene-owned types only. Public signatures must not contain:

- `org.cef.*`;
- `io.github.trethore.jcefgithub.*`;
- internal implementation classes;
- Minecraft or Fabric types in the common package.

Replace public JCEF customization points with stable Graphene abstractions, including:

- browser options;
- request-context options;
- frame-rate and transparency settings;
- navigation and load events;
- script execution;
- bridge requests, events, and subscriptions;
- runtime status and remote-debugging information.

Advanced implementation-specific behavior should remain internal instead of becoming a permanent compatibility
obligation.

### 4. Introduce platform ports in common

Replace direct Fabric and Minecraft access with small internal service interfaces:

```text
GrapheneLifecycle
GrapheneMainThreadExecutor
GrapheneModResolver
GrapheneNativeWindow
GrapheneWindowMetrics
GrapheneStartupPresenter
GrapheneFileDialogPresenter
GrapheneJsDialogPresenter
```

Requirements:

- Common owns the contracts.
- Fabric owns their implementations.
- Services are installed explicitly by `FabricBootstrap`.
- Common initialization fails clearly if no platform services are installed.
- Tests use fake services without loading Minecraft.
- Avoid one large platform facade and avoid reflection-based Fabric access.

### 5. Migrate configuration and portable value types

Move the old configuration APIs into common and rename them under `io.github.trethore.graphene.api.config`.

Modernize them by:

- preserving immutability;
- validating paths, ports, ranges, and identifiers during construction;
- replacing nullable state with `Optional`, `OptionalInt`, or explicit defaults where appropriate;
- documenting which settings are per-consumer and which are process-global;
- preserving deterministic conflict detection when multiple consumer mods request incompatible global settings.

Move and adapt the old configuration tests before migrating runtime code.

### 6. Migrate bridge, HTTP, resource, and URL subsystems

Move the following into common:

- bridge protocol and message codec;
- outbound queue and overflow policy;
- pending request lifecycle;
- handler and subscription registries;
- JSON bridge support;
- HTTP server and per-mod mounts;
- classpath and HTTP asset URL generation;
- MIME type detection;
- bridge JavaScript resources.

Make bridge code depend on a Graphene-owned browser endpoint rather than a concrete CEF browser:

```java
interface BrowserEndpoint {
    void executeScript(String script, String sourceUrl);

    String currentUrl();
}
```

Port tests for malformed messages, queue overflow, request cleanup, subscription cleanup, mount isolation, SPA fallback,
URL escaping, path traversal, and MIME resolution.

### 7. Build an explicit common runtime

Replace static construction through the old `GrapheneCoreServices.get()` pattern with one explicitly installed runtime.

The common runtime should own:

- consumer registration by mod ID;
- per-consumer configuration;
- global configuration merging;
- initialization state;
- HTTP runtime;
- bridge runtime;
- browser registry;
- shutdown coordination.

Use explicit states:

```text
NEW -> STARTING -> RUNNING -> STOPPING -> STOPPED
                  |
                  -> FAILED
```

Expose asynchronous initialization using `CompletionStage` or `CompletableFuture`. Blocking initialization may exist as
a deliberate convenience method but must not obscure failures or deadlock the Minecraft thread.

### 8. Migrate JCEF behind an internal adapter

Move CEF installation, app creation, client creation, handlers, message routing, and shutdown into `common/internal/cef`
where they do not require Minecraft UI types.

Create internal interfaces around browser creation and runtime control so the rest of common does not depend throughout
on concrete `org.cef` classes.

Preserve and improve the old behavior:

- asynchronous native download and startup;
- cleanup after partial initialization;
- idempotent shutdown;
- bounded wait for CEF termination;
- HTTP server cleanup on CEF failure;
- observable startup failures;
- one owned startup executor that is terminated during shutdown;
- no remaining CEF process after Minecraft exits.

Move Minecraft-dependent CEF dialogs and startup overlays to the Fabric package behind the platform presenter
interfaces.

### 9. Separate browser capture from Minecraft rendering

Split the old `GrapheneBrowser` and `BrowserSurface` responsibilities.

Common owns:

- off-screen browser lifecycle;
- navigation and history;
- load events;
- bridge attachment;
- JavaScript execution;
- viewport and coordinate mapping;
- browser frame capture;
- screenshots;
- platform-neutral input commands.

Fabric owns:

- `GuiGraphics` rendering;
- OpenGL/LWJGL texture allocation and upload;
- Minecraft profiling sections;
- cursor conversion;
- GLFW native window lookup;
- Minecraft scale factors;
- Minecraft mouse and keyboard event conversion.

Introduce a Graphene-owned frame contract containing dimensions, dirty regions, pixel data, and a frame sequence. JCEF
callbacks publish frames; the Fabric renderer consumes them on the correct render thread.

### 10. Implement Fabric bootstrap and adapters

Replace the placeholder bootstrap with a client-only Fabric entrypoint that:

- creates and installs all Fabric platform services;
- resolves anchor classes and explicit IDs to loaded Fabric mods;
- validates consumer IDs;
- closes registration at the intended lifecycle point;
- starts Graphene after client startup when consumers exist;
- shuts Graphene down during client stopping;
- schedules Minecraft work on the client thread;
- provides the native window handle and scale information.

Keep consumer registration available through common public API, but place Fabric-specific anchor-class resolution in the
Fabric adapter.

### 11. Rebuild the Fabric UI API

Migrate and modernize:

- `BrowserSurface`;
- `BrowserSurfaceInputAdapter`;
- `GrapheneWebViewWidget`;
- `GrapheneScreens`;
- screen ownership cleanup;
- JavaScript dialogs;
- file and folder dialogs;
- native download overlay;
- screen mixins.

Fabric public APIs may use Minecraft types. Common public APIs may not.

Prefer a context-oriented usage model:

```java
GrapheneContext context = Graphene.context("consumer-mod-id");

BrowserSurface surface = BrowserSurface.builder(context)
        .url(context.assets().url("web/index.html"))
        .size(800, 600)
        .build();
```

The final API names should be reviewed during implementation, but JCEF must remain hidden regardless of naming.

### 12. Standardize metadata and resources

Update Fabric resources to consistently use:

```text
project name: Graphene
mod id: grapheneui
asset namespace: grapheneui
Java package root: io.github.trethore.graphene
```

Change `fabric.mod.json` to:

- use a `client` entrypoint rather than `main`;
- reference the new `FabricBootstrap` package;
- retain `grapheneui` as the ID;
- reference `assets/grapheneui/icon.png`;
- reference `grapheneui.mixins.json`;
- declare the supported Minecraft, Java, loader, and Fabric API versions.

Move bridge scripts into common resources and ensure embedding common places them in the final Fabric JAR without
duplication.

### 13. Port tests in dependency order

Migrate tests incrementally:

1. Configuration validation and merging
2. URL and MIME handling
3. Viewport and coordinate mapping
4. Bridge codec, queues, requests, and subscriptions
5. HTTP server and asset mounts
6. Platform-neutral keyboard/input mapping
7. CEF handler behavior using fakes or mocks
8. Runtime state and failure cleanup
9. Fabric adapter tests where Minecraft-independent testing is possible
10. In-game debug smoke tests

Each subsystem must pass before dependent subsystems are migrated.

### 14. Restore the debug client

Add a Fabric debug source set after production code is stable. Migrate:

- debug key bindings;
- debug browser screens;
- automated bridge pages;
- focus and mouse pages;
- dialog tests;
- test assets and styles;
- configurable debug logging;
- a `debugClient` Loom run configuration.

Exclude debug classes and resources from the release artifact.

### 15. Update documentation

Rewrite documentation for:

- the versioned artifact model;
- installation for Minecraft 1.21.11;
- `io.github.trethore.graphene` imports;
- `grapheneui` Fabric dependency declaration;
- initialization and consumer registration;
- context and asset APIs;
- browser and surface lifecycle;
- bridge requests and events;
- HTTP mounts;
- remote debugging;
- threading and shutdown;
- migration from the old `tytoo.grapheneui` API.

Include a breaking-change table rather than compatibility wrappers.

### 16. Final cleanup and verification

Remove placeholders after their replacements compile:

- `packages/common/src/main/java/io/github/trethore/graphene/api/Main.java`
- `packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/mixin/ExampleMixin.java`

Run automated verification:

```bash
./gradlew spotlessApply
./gradlew check
```

Run the client manually:

```bash
./gradlew :packages:fabric-1.21.11:runClient
```

Do not run game launch tasks as part of automated agent verification.

## Recommended Delivery Phases

### Phase 1: Foundation

- Migration inventory
- Gradle dependency model
- Package conventions
- Public API boundaries
- Platform ports
- Configuration, URL, HTTP, bridge, and portable tests

### Phase 2: Runtime

- Runtime state machine
- JCEF adapter and installer
- Browser lifecycle and frame capture
- Failure cleanup and shutdown tests

### Phase 3: Fabric integration

- Fabric bootstrap and platform services
- GPU renderer and browser surface
- Input, widgets, screens, dialogs, overlays, and mixins
- Metadata and resources

### Phase 4: Productization

- Debug source set
- Documentation
- Publishing metadata
- Complete automated and manual validation

Avoid copying all old classes in one change. Each phase should compile and each migrated subsystem should retain or
improve its test coverage.

## Acceptance Criteria

- All production Java packages use `io.github.trethore.graphene`.
- The Fabric mod ID and asset namespace are consistently `grapheneui`.
- `packages/common` has no imports from `net.minecraft.*` or `net.fabricmc.*`.
- `packages/common` builds and tests without Loom.
- Common public APIs expose no JCEF, Minecraft, Fabric, or internal implementation types.
- `packages/fabric-1.21.11` depends on common in one direction only.
- The final Minecraft 1.21.11 release is one remapped Fabric JAR containing common and Fabric code.
- JCEF classes and resources are packaged exactly once.
- Consumer configurations remain isolated by mod ID.
- Conflicting process-global settings fail deterministically.
- Initialization and shutdown are thread-safe, observable, and idempotent.
- Failed initialization releases CEF, HTTP, browser, and executor resources.
- Browser frame upload and rendering occur on the correct Minecraft thread.
- Bridge, URL, HTTP, viewport, input, dialog, and lifecycle behavior is covered by migrated tests.
- Debug-only classes and assets are absent from release artifacts.
- `./gradlew check` passes.
- The debug client demonstrates feature parity with the old repository.
- Closing Minecraft leaves no CEF subprocess or Graphene-owned non-daemon resource running.
- No files under `references/` are modified.

## Inspected Sources

- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/api/GrapheneCore.java:1-35` - Fabric lifecycle,
  mod resolution, configuration registry, and runtime ownership are combined.
- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/api/GrapheneCore.java:137-221` - registration
  closing, Fabric mod lookup, and Minecraft identifier validation must move to Fabric adapters.
- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/internal/cef/GrapheneCefRuntime.java:3-35` - JCEF
  runtime currently depends directly on Fabric lifecycle and Minecraft helpers.
- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/internal/cef/GrapheneCefRuntime.java:108-155` -
  existing asynchronous initialization behavior.
- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/internal/cef/GrapheneCefRuntime.java:335-378` -
  existing partial-initialization cleanup behavior.
- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/api/surface/BrowserSurface.java` - public browser
  behavior and Minecraft rendering are currently combined.
- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/internal/browser/GrapheneBrowser.java` - JCEF
  browser capture is coupled to Minecraft rendering, cursors, and window access.
- `references/trethore-graphene-main/src/client/java/tytoo/grapheneui/api/GrapheneHandle.java` - old consumer-scoped API
  to preserve conceptually while removing JCEF leakage.
- `packages/common/build.gradle.kts:8-15` - current internal JCEF GitHub dependency.
- `packages/fabric-1.21.11/build.gradle.kts:38-45` - current Minecraft/Fabric dependencies and common embedding.
- `references/io.github.trethore-jcefgithub-146.0.10.3/jcefgithub_build_meta.json` - current native artifact platforms
  and hashes.

## Remaining Questions

No blocking architectural questions remain. API class names and exact artifact naming can be finalized during the
foundation phase without changing the confirmed package, mod ID, dependency, or release-artifact decisions.
