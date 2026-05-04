# Repository Guidelines

Graphene is a modern, client-side, Chromium-based UI library for Fabric Minecraft mods.
Its goal is to provide a simple yet powerful API for mod developers to create rich, web-based user interfaces in Minecraft using JCEF.

## Overview

The repository is split into a shared module and version-specific Fabric modules:

```text
/
├── common/                              # Loader- and Minecraft-free shared API and internals.
│   └── src/
│       ├── main/java/tytoo/grapheneui/
│       │   ├── api/                     # Public API that can be shared across supported versions.
│       │   │   ├── bridge/              # Java-to-browser bridge events, requests, JSON helpers, and subscriptions.
│       │   │   ├── config/              # Consumer-facing runtime, container, HTTP, and remote debugging configuration.
│       │   │   ├── runtime/             # Public runtime and embedded HTTP server handles.
│       │   │   ├── surface/             # Version-neutral surface config and listeners.
│       │   │   └── url/                 # Helpers for building asset and classpath URLs.
│       │   └── internal/                # Shared implementation details without Minecraft/Fabric dependencies.
│       │       ├── bridge/              # Bridge protocol, routing, codecs, queues, and request lifecycle internals.
│       │       ├── browser/             # Version-neutral browser input, focus, render, and surface state.
│       │       ├── cef/                 # Shared CEF/JCEF setup, handlers, dialogs, and startup state.
│       │       ├── core/                # Shared service wiring and runtime state.
│       │       ├── event/               # Internal load and title event buses.
│       │       ├── http/                # Embedded HTTP server runtime implementation.
│       │       ├── input/               # Version-neutral input mapping utilities.
│       │       ├── logging/             # Debug logging selection and output helpers.
│       │       ├── platform/            # Platform and environment detection.
│       │       ├── resource/            # Resource metadata helpers such as MIME type detection.
│       │       └── url/                 # Internal URL builders and scheme-specific URL implementations.
│       └── test/                        # Shared unit tests.
├── fabric-1.21.11/                      # Fabric adapter for Minecraft 1.21.11.
│   ├── gradle.properties                # Version-specific Minecraft and Fabric API versions.
│   └── src/
│       ├── client/                      # Fabric client mod entrypoints, resources, mixins, and Minecraft adapters.
│       │   ├── java/tytoo/grapheneui/
│       │   │   ├── api/                 # Fabric/Minecraft-facing public API, screens, surfaces, widgets, and runtime access.
│       │   │   └── internal/            # Fabric/Minecraft integration, rendering, mixins, screens, and MC wrappers.
│       │   └── resources/               # Fabric metadata, mixin configs, assets, and bundled browser resources.
│       ├── debug/                       # Debug mod used for manual in-game testing.
│       └── test/                        # Version-specific tests.
├── references/                          # Unpacked dependency sources for browsing and reference.
├── docs/                                # User and contributor documentation.
├── build.gradle.kts
├── gradle.properties                    # Shared Gradle, loader, and dependency versions.
└── settings.gradle.kts                  # Included modules.
```

## General Coding Conventions

- Target Java 21, use 4-space indentation, and keep packages under `tytoo.grapheneui*`.
- Prefer explicit types over `var`, and use descriptive names instead of one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors, overridden
  methods, public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.
- When adding shared utilities, express behavior through clear method names and arguments rather than abstract hierarchies.
- Avoid comments unless documentation is explicitly requested.
- Keep edits minimal and consistent with the surrounding style; avoid unrelated refactors or formatting-only changes.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.
- If requirements are unclear or infeasible, ask for clarification before proceeding.

## Java 21 Expectations

- Assume Java 21 at runtime; use only stable features and avoid preview or incubator APIs.
- Use modern Java 21 standard-library utilities, such as Streams, Optional, and records, when they improve clarity.
- Use descriptive names such as `ignored` for intentionally unused variables, parameters, and caught exceptions.
- When intentionally ignoring a caught exception, keep a short explanatory comment in the catch block.
- Maintain explicit, readable control flow; avoid clever constructs that harm comprehension.

## Multi-Version Module Rules

- Put version-neutral API, bridge, URL, config, HTTP, logging, CEF, resource, and pure Java logic in `common/`.
- `common/` must not import Minecraft, Mojang, Fabric, or Mixin classes. The `:common:checkNoMinecraftImports` task enforces this for common main sources.
- Put Minecraft, Fabric, rendering, screen, widget, mixin, entrypoint, and mapping-specific code in the matching `fabric-<minecraft-version>/` module.
- A new supported Minecraft version should be added as a new module, for example `fabric-1.21.x/`, instead of weakening existing version-specific code with compatibility branches.
- Keep shared public API stable in `common/src/main/java/tytoo/grapheneui/api/` unless a breaking API change is intentional and documented.
- Version modules may expose Fabric/Minecraft-facing public API where types necessarily mention Minecraft classes.

## Minecraft Integration Rules

- Fabric version modules use official Mojang mappings for their configured Minecraft version.
- Use modern Fabric and Minecraft methods for the target module version, such as `Identifier.fromNamespaceAndPath(String string, String string2)` where applicable.
- Route Minecraft client singleton access through `tytoo.grapheneui.internal.mc.*` helpers inside version modules instead of calling `Minecraft.getInstance()` directly.
- Place version-specific assets, mixin configs, and JSON metadata within the owning module, such as:
  - `fabric-1.21.11/src/client/resources/`
  - `fabric-1.21.11/src/debug/resources/`
- Keep identifiers in the `GrapheneCore.ID` or `GrapheneDebugClient.ID` namespace as appropriate.

## Dependencies & External Sources

- Shared Fabric Loader, Loom, JCEF, LWJGL, and JUnit versions are in root `gradle.properties`.
- Minecraft and Fabric API versions live in the relevant version module, such as `fabric-1.21.11/gradle.properties`.
- Fabric Loom integrates official Mojang mappings into version module source sets and remaps game classes during packaging.
- Keep each version module's Minecraft and Fabric API versions aligned before updating APIs.
- `io.github.trethore:jcefgithub` is this project's own JCEF library, published on GitHub Packages; browse its unpacked sources in `references/`.
- Library sources are fetched through the `sourceDeps` configuration in the version module and unpacked using `./gradlew unpackSources` into `references/<library>`.

## Testing & Verification

- Run `./gradlew check` to catch compile, test, packaging, and common-module dependency errors.
- Use focused checks when needed:
  - `./gradlew :common:test`
  - `./gradlew :common:checkNoMinecraftImports`
  - `./gradlew :fabric-1.21.11:test`
  - `./gradlew :fabric-1.21.11:compileClientJava`
- Do not run long-running Gradle tasks, such as game launches, yourself. Provide the exact command for the user instead, for example, `./gradlew :fabric-1.21.11:runDebugClient`.
- Document manual validation steps and remaining risks before completing work.

## PRs and Commits

- For PRs, use the `.github/pull_request_template.md` template.
- For commits, follow the conventional commit format.
