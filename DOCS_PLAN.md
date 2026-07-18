# Graphene Documentation Plan

## Goal

Replace `docs/` with concise, GitHub-rendered Markdown documentation for mod developers consuming Graphene.

The documentation will use the Diataxis categories while applying progressive disclosure:

1. Get a browser UI on screen.
2. Make the UI communicate with Java.
3. Introduce advanced browser behavior only when the developer needs it.
4. Keep precise defaults and API details in compact reference pages.

The documentation is not intended to duplicate every public type or implementation detail. Developers and code-search tools can use the source and Javadocs for uncommon or low-level cases.

## Audience and scope

The audience is Fabric mod developers who are new to Graphene or need guidance for a common integration task.

The documentation will cover:

- Installing Graphene and checking loader/Minecraft compatibility.
- Registering a consumer and understanding runtime startup.
- Loading packaged HTML, CSS, JavaScript, and other assets.
- Displaying a web interface in a Minecraft screen.
- Communicating between Java and JavaScript.
- Choosing between a widget, surface, and browser session.
- Browser navigation, options, events, and lifecycle.
- Local frontend development, SPA fallback, and DevTools.
- Security-sensitive policies for bridge exposure, navigation, files, downloads, dialogs, and context menus.
- Common failures and their fixes.
- A curated reference for the core Java and JavaScript APIs.

The following are outside this documentation:

- Contributor setup and repository architecture rules.
- Running Graphene's debug client and internal test suite.
- Source-unpacking and internal JCEF implementation notes.
- Exhaustive class-by-class API documentation.
- Framework-specific tutorials.
- Documentation-site generators, custom themes, and hosted documentation infrastructure.

Contributor information will eventually live in a root `CONTRIBUTING.md`.

## Documentation structure

```text
docs/
  README.md
  images/
    threejs-showcase.png
    bridge-round-trip.png
    devtools-inspection.png

  tutorials/
    first-web-screen.md
    connect-java-and-javascript.md

  how-to/
    manage-assets-and-frontend-development.md
    control-and-observe-the-browser.md
    render-a-custom-browser-surface.md
    configure-browser-policies.md
    use-devtools.md
    manage-browser-lifecycle.md
    troubleshoot.md

  explanation/
    architecture-and-runtime.md
    browser-layers.md
    assets-origins-and-bridge-security.md

  reference/
    compatibility-and-installation.md
    core-java-api.md
    javascript-bridge-api.md
    configuration-and-defaults.md
```

This structure contains 17 pages including section indexes represented by the root landing page. It is small enough to maintain manually while covering the normal path from installation to advanced integration.

## Page specifications

### `docs/README.md`

Purpose: orient a new developer and send them to the shortest relevant path.

Content:

- One-paragraph description of Graphene.
- The Three.js showcase image.
- A concise statement that Graphene embeds Chromium through JCEF and is a client-side UI library.
- Current compatibility summary with a link to the compatibility reference.
- Three progressive entry points:
  - **Start here** -> build the first web screen.
  - **Make it interactive** -> connect Java and JavaScript.
  - **Solve a specific task** -> how-to guides.
- Links to the explanation and reference sections.
- A short capabilities list without reproducing the full API.
- Links to Maven Central, Modrinth, and GitHub Releases.

The landing page must remain scannable and should not become a complete guide itself.

### Tutorials

Tutorials are linear, tested learning experiences. Each tutorial starts from a defined state, produces a visible result, and ends with links to deeper material.

#### `tutorials/first-web-screen.md`

Outcome: a Fabric mod displays a packaged HTML/CSS/JavaScript page inside a Minecraft `Screen`.

Content:

1. Prerequisites and compatibility link.
2. Add `mavenCentral()` and the Graphene dependency:

   ```kotlin
   repositories {
       mavenCentral()
   }

   dependencies {
       modImplementation("io.github.trethore:graphene-ui:<version>")
   }
   ```

3. Register the consuming mod with `Graphene.register(...)` during client initialization.
4. Place a minimal page under `assets/<mod-id>/...`.
5. Build its URL with `context.appAssets().url(...)`.
6. Create and add a `GrapheneWebViewWidget` to a screen.
7. Explain automatic widget closure in one short lifecycle note.
8. Run the mod and verify the page appears.
9. Link to the bridge tutorial and relevant how-to pages.

The tutorial will use plain HTML, CSS, and JavaScript with no frontend build tooling.

#### `tutorials/connect-java-and-javascript.md`

Outcome: the page and Java exchange typed JSON events and request/response messages in both directions.

Content:

1. Continue from the first tutorial.
2. Wait for `globalThis.grapheneBridge` readiness.
3. Send a JavaScript event and handle it in Java.
4. Send a JavaScript request and return a Java response.
5. Register a JavaScript handler and issue a request from Java.
6. Use `emitJson`, `requestJson`, `onEventJson`, and `onRequestJson` for concise typed examples.
7. Store and unsubscribe `GrapheneSubscription` instances when the owning UI closes.
8. Explain channel naming and the reserved `graphene:` prefix.
9. Show expected success output using the bridge round-trip image.
10. Link to the bridge reference and security explanation.

### How-to guides

How-to guides are task-oriented and assume the reader has completed or understood the first tutorial.

#### `how-to/manage-assets-and-frontend-development.md`

Cover:

- Packaged consumer assets through `context.appAssets()`.
- Shared classpath assets through `context.classpathAssets()`.
- When HTTP assets are needed.
- Enabling `GrapheneHttpConfig`.
- Using `fileRoot` for an edit-refresh development loop.
- Configuring `spaFallback` for client-side routing.
- Switching between development and packaged assets without changing browser code.
- A short framework note: React, Vue, Svelte, Three.js, and other browser libraries work when their build output is served as ordinary web assets; Graphene does not require a specific framework or bundler.

#### `how-to/control-and-observe-the-browser.md`

Cover the common `BrowserSession` operations:

- Navigate, back, forward, reload, and stop.
- Read URL, title, loading state, and history availability.
- Execute scripts.
- Change zoom.
- Find text.
- Subscribe to load, URL, title, console, frame, and download events.
- Note callback/thread behavior and subscription cleanup.

#### `how-to/render-a-custom-browser-surface.md`

Cover:

- When `BrowserSurface` is more appropriate than `GrapheneWebViewWidget`.
- Creating, rendering, resizing, and closing a surface.
- Logical dimensions versus browser pixel resolution.
- Automatic resolution and fixed resolution.
- Mapping coordinates to browser pixels.
- Forwarding low-level input with `BrowserSurfaceInputAdapter`.

This guide will warn that most screen-based interfaces should use `GrapheneWebViewWidget` instead.

#### `how-to/configure-browser-policies.md`

Cover policy examples for:

- Restricting bridge exposure by document source or exact origin.
- Allowing, cancelling, externally opening, or consumer-managing navigation.
- Enabling and filtering context-menu items.
- Cancelling, directly saving, or prompting for downloads.
- Providing context-menu, file-dialog, and JavaScript-dialog presenters.
- Enabling browser file access only when required.

The guide will lead with secure defaults and explain that policy callbacks must be fast, thread-safe, and non-blocking where required by the API.

#### `how-to/use-devtools.md`

Cover:

- Enabling remote debugging with a random port.
- Configuring allowed origins.
- Waiting for runtime initialization.
- Checking `runtime().devTools().isEnabled()`.
- Discovering the target for a `BrowserSession`.
- Opening `DevToolsPageTarget.inspectorUri()` with the platform browser.
- Handling disabled, unavailable, missing, ambiguous, and discovery failures.
- The DevTools inspection image.

#### `how-to/manage-browser-lifecycle.md`

Cover:

- Registration timing and the closing of registration at platform startup.
- Waiting on `runtime().initialization()` before creating low-level sessions when necessary.
- Automatic widget closure when a screen closes.
- Disabling automatic closure for intentionally persistent widgets.
- Closing widgets, surfaces, sessions, input adapters, and subscriptions.
- Avoiding use-after-close and browser creation before the runtime reaches `RUNNING`.

#### `how-to/troubleshoot.md`

Use a symptom -> cause -> fix format for:

- Graphene consumer was not registered.
- Registration happened too late.
- No loaded mod matches the supplied mod ID.
- The runtime is not running or initialization failed.
- The JCEF runtime cannot be installed or started.
- The page is blank because the asset URL/path is wrong.
- The JavaScript bridge is absent because the document is not allowed.
- HTTP assets are disabled or the shared HTTP configuration conflicts.
- Global configuration conflicts between consuming mods.
- Input does not work because focus or input forwarding is missing.
- Rendering is blurry because logical and browser resolutions are confused.
- DevTools is disabled or cannot identify a target.
- A widget or browser remains alive after its screen closes.

Include the location of the browser runtime and its `logs.txt` file as a final diagnostic step.

### Explanations

Explanation pages teach the mental model rather than prescribing a sequence of steps.

#### `explanation/architecture-and-runtime.md`

Explain:

- Graphene as a client-side library using an off-screen Chromium browser through JCEF.
- Loader-independent common APIs versus loader-specific Minecraft integration.
- One process-wide browser runtime shared by registered consumer mods.
- Consumer-scoped `GrapheneContext` objects.
- Container-scoped versus process-wide configuration.
- Why incompatible global contributions fail early.
- Runtime states from `NEW` through `RUNNING`, failure, and shutdown.

#### `explanation/browser-layers.md`

Explain the ownership and abstraction hierarchy:

```text
GrapheneWebViewWidget -> BrowserSurface -> BrowserSession -> JCEF browser
```

Cover what each layer owns, when each should be selected, and who closes it. Also explain rendering frames, GUI scale, browser resolution, focus, and input flow at a conceptual level.

#### `explanation/assets-origins-and-bridge-security.md`

Explain:

- Packaged app assets, classpath assets, and HTTP-served assets.
- Consumer namespaces and normalized asset paths.
- Why origin and document source matter to an embedded browser.
- Why the bridge is exposed only to Graphene-owned documents by default.
- Risks of exposing the bridge to remote content.
- Why the `graphene:` channel namespace is reserved.
- Why browser file access, downloads, external navigation, and dialogs are opt-in or restricted by default.

### Reference

Reference pages must be compact, factual, and optimized for lookup.

#### `reference/compatibility-and-installation.md`

Contain:

- A table of supported loader and Minecraft combinations.
- Java requirement.
- Current Graphene version.
- Maven Central coordinates.
- Gradle repository and dependency snippets.
- Required runtime dependencies such as Fabric API.
- Links to Maven Central, Modrinth, and GitHub Releases.
- Artifact naming used by GitHub Releases.

The table is the single source of truth for current compatibility. Other pages link to it rather than copying full version details. Its columns will support additional loaders and Minecraft versions without changing the surrounding documentation structure.

#### `reference/core-java-api.md`

Provide a curated table grouped by purpose:

- Entry and context: `Graphene`, `GrapheneContext`, `GrapheneSubscription`.
- Browser: `BrowserSessions`, `BrowserSession`, `BrowserOptions`.
- Bridge: `GrapheneBridge`, `GrapheneBridgeJson`, bridge handlers and exceptions.
- Assets: `GrapheneAssetUrls`, `GrapheneClasspathUrls`, `AssetId`.
- Runtime and DevTools: `GrapheneRuntime`, `GrapheneHttpServer`, `GrapheneDevTools`, `DevToolsPageTarget`.
- Fabric UI: `GrapheneWebViewWidget`, `BrowserSurface`, `BrowserSurfaceInputAdapter`, `GrapheneScreens`.
- Policy entry points for bridge, navigation, downloads, context menus, and dialogs.

Each entry will contain one sentence describing when it is used and link to its source file. The page will not list every record, enum value, getter, listener, or exception.

#### `reference/javascript-bridge-api.md`

Document the injected `globalThis.grapheneBridge` object:

- `isReady()`
- `ready()`
- `onReady(listener)`
- `on(channel, listener)` and its unsubscribe function
- `off(channel, listener)`
- `handle(channel, handler)` and its unregister function
- `emit(channel, payload)`
- `request(channel, payload)`

Also document:

- Promise behavior.
- JSON-compatible payloads.
- Event versus request semantics.
- Error propagation.
- Handler replacement behavior.
- Document navigation resetting page-side registrations.
- Reserved channels.

#### `reference/configuration-and-defaults.md`

Provide tables for:

- `GrapheneConfig` and `GrapheneContainerConfig`.
- `GrapheneGlobalConfig`.
- `GrapheneHttpConfig`.
- `GrapheneRemoteDebugConfig`.
- `BrowserOptions`.
- Default bridge, navigation, download, context-menu, file-access, HTTP, and remote-debugging behavior.
- Which settings are consumer-scoped, merged, combined, or required to match across consumers.

This page is the canonical source for defaults; tutorials and how-to guides should link to it rather than reproduce complete default tables.

## Progressive disclosure rules

Every page will follow these rules:

- Put the common path before customization and edge cases.
- Begin with the outcome or question answered by the page.
- Use the highest-level API that solves the task.
- Introduce `GrapheneWebViewWidget` before `BrowserSurface`, and `BrowserSurface` before direct `BrowserSession` use.
- Introduce packaged assets before HTTP development hosting.
- Introduce bridge events before request/response handling.
- Keep security warnings beside the setting that creates the risk.
- Move exhaustive defaults into reference tables.
- End tutorials and how-to guides with a short **Next steps** section.
- Link to source for uncommon details instead of copying implementation code.

## Writing conventions

- Use plain Markdown supported by GitHub.
- Keep headings descriptive and stable for deep links.
- Use relative links within the repository.
- Use Java and Kotlin Gradle code fences with complete imports where needed.
- Use `<version>` only in reusable installation snippets; show the current concrete version in the compatibility reference.
- Use `<mod-id>` for consumer-owned asset paths.
- Make code examples minimal but compilable in their stated context.
- State ownership and cleanup requirements wherever an object is `AutoCloseable` or returns a subscription.
- State callback threading or non-blocking requirements wherever they affect correct use.
- Do not document internal packages as consumer APIs.
- Do not promise support beyond the compatibility table.
- Avoid repeating version numbers, defaults, and compatibility data across pages.

## Images

The following screenshots will be captured and saved under `docs/images/`.

### `threejs-showcase.png`

- Show a visually striking Three.js scene rendered by Graphene inside Minecraft.
- Keep enough Minecraft UI visible to establish that the scene is in-game.
- Use on the documentation landing page.

### `bridge-round-trip.png`

- Open the debug client's bridge test page.
- Show successful JavaScript -> Java and Java -> JavaScript interactions in the visible log.
- Use in the bridge tutorial.

### `devtools-inspection.png`

- Show Minecraft running a Graphene interface with Chromium DevTools open beside it.
- Select a meaningful DOM element or show a useful console interaction.
- Use in the DevTools how-to.

Images should be PNG, readable at GitHub's normal content width, and contain no unrelated desktop or personal information. Each image must have useful alt text in Markdown.

## Existing documentation migration

- Replace the current `docs/README.md` with the new landing page.
- Remove `docs/running.md` from consumer documentation because it describes repository and debug-client workflows.
- Remove `docs/references.md` from consumer documentation because source unpacking is a contributor task.
- Preserve no links to the removed pages.
- Reintroduce the useful contributor material later in the root `CONTRIBUTING.md`.

## Implementation order

### Phase 1: onboarding path

1. Create the directory structure.
2. Write `reference/compatibility-and-installation.md`.
3. Write `tutorials/first-web-screen.md` and verify its code in a minimal consumer mod.
4. Write `tutorials/connect-java-and-javascript.md` and verify all four bridge directions.
5. Replace `docs/README.md` and connect the onboarding path.

### Phase 2: common tasks

1. Write asset and frontend-development guidance.
2. Write browser control and observation guidance.
3. Write lifecycle guidance.
4. Write DevTools guidance.
5. Write troubleshooting guidance from failures encountered while testing the tutorials.

### Phase 3: advanced use and mental model

1. Write the custom surface guide.
2. Write the browser policy guide.
3. Write the three explanation pages.

### Phase 4: compact reference and polish

1. Write the curated Java API reference.
2. Write the JavaScript bridge reference.
3. Write the configuration/default tables.
4. Add the three screenshots and alt text.
5. Remove the obsolete contributor-oriented pages.
6. Check all links, snippets, terminology, and repeated facts.

## Verification

Before the documentation rewrite is complete:

- Follow the first tutorial from a clean Fabric consumer project.
- Verify the dependency resolves from Maven Central.
- Verify packaged HTML, CSS, and JavaScript load from the documented paths.
- Verify the bridge tutorial in both directions.
- Verify the HTTP file-root and SPA examples.
- Verify the DevTools flow and screenshot against the documented configuration.
- Compile every Java snippet that is presented as complete code.
- Check every relative Markdown link and image path.
- Run `./gradlew spotlessApply` after editing Markdown.
- Run `./gradlew check` before completion.

## Definition of done

The documentation rewrite is complete when a new Graphene consumer can:

1. Find the supported loader, Minecraft, Java, and Graphene versions.
2. Add the Maven Central dependency.
3. Register their mod correctly.
4. Display a packaged web page in a Minecraft screen.
5. Exchange events and requests between Java and JavaScript.
6. Find a focused guide for assets, browser control, lifecycle, custom rendering, policies, and DevTools.
7. Understand the runtime, browser-layer, and security mental models.
8. Look up core API entry points and defaults without reading an exhaustive manual.
9. Diagnose the common failures listed in the troubleshooting guide.

The finished `docs/` must remain concise enough that compatibility, defaults, and examples can be maintained alongside code changes without requiring a separate documentation toolchain.
