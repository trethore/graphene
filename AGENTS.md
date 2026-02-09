# Repository Guidelines

Graphene is a modern, client-side, Chromium-based UI library for Minecraft 1.21.11 that runs on the Fabric mod loader.
Its goal is to provide a simple yet powerful API for mod developers to create rich, web-based user interfaces in
Minecraft using JCEF.

## Overview

Here is the structure of the repository:

```text
/
├── libs-src/                           # Unpacked dependency sources for browsing and reference.
│   ├── fabric/
│   ├── minecraft/
│   └── <lib-name>/
├── src/
│   ├── client/                         # Client core API and resources (main library code).
│   ├── debug/                          # Debug mod used for manual testing (for example, opening a UI).
│   └── test/                           # Unit tests using JUnit 6 (currently a placeholder).
├── .gitignore
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## General Coding Conventions

- Target Java 25, use 4-space indentation, and keep packages under `tytoo.grapheneui*`.
- Use PascalCase for classes, camelCase for methods and fields, and UPPER_SNAKE_CASE for constants.
- Use explicit types instead of `var`, and prefer descriptive names over one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors, overridden
  methods, public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.
- When adding shared utilities, express behavior through clear method names and arguments rather than abstract hierarchies.
- Avoid comments unless documentation is explicitly requested.
- Keep edits minimal and consistent with surrounding style; avoid unrelated refactors or formatting-only changes.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.
- If requirements are unclear or infeasible, ask for clarification before proceeding.

## Java 25 Expectations

- Assume Java 25 at runtime; use only stable features and avoid preview or incubator APIs.
- Use modern Java 25 standard-library utilities (Streams, Optional, records) when they improve clarity.
- Prefer unnamed variables (`_`) for intentionally unused variables, parameters, and caught exceptions.
- When intentionally ignoring a caught exception, keep a short explanatory comment in the catch block.
- Maintain explicit, readable control flow; avoid clever constructs that harm comprehension.

## Minecraft Integration Rules

- The codebase targets Fabric for Minecraft 1.21.11 with official Mojang mappings; use APIs that exist in this combination.
- Prefer modern Fabric/Minecraft methods such as `Identifier.fromNamespaceAndPath(String string, String string2)` and
  up-to-date rendering APIs; avoid deprecated signatures.
- Place new assets, mixin configs, and JSON metadata within `src/client/resources/` or `src/debug/resources/`,
  keeping identifiers in the `GrapheneCore.ID` or `GrapheneDebugClient.ID` namespace as appropriate.
- Integrate through established abstractions unless explicitly extending them.
- Never reference loaders, mappings, or game versions beyond the configured target without explicit user approval.

## Testing & Verification

- Do not run Gradle commands yourself; instead provide the exact command for the user to execute and state tooling limitations clearly.
- Encourage running `./gradlew compileJava` after changes, `./gradlew build` for full validation, and `./gradlew runDebugClient` to test UI flows.
- Document manual validation steps and remaining risks before completing work.

## Dependencies & External Sources

- Fabric Loader and Fabric API are versioned in `gradle.properties`; Fabric Loom integrates official Mojang mappings
  into the client source set and remaps game classes during packaging. Keep these aligned with Minecraft `1.21.11` before updating APIs.
- `me.tytoo:jcefgithub` is this project's own JCEF library, published on GitHub Packages; browse its unpacked sources in `libs-src/`.
- Library sources are fetched through the `sourceDeps` configuration (see `build.gradle.kts`) and unpacked per library
  using `./gradlew unpackSources` into `libs-src/<library>`. Use these sources to explore library source code.

## Pull Requests & Commits

- Keep PRs focused on a single concern and avoid unrelated cleanups.
- Provide clear summaries, rationale, and manual test steps; include visuals for UI changes when relevant.
- Use Conventional Commit conventions (e.g., `feat(ui): add slider snap support`) and flag breaking API changes early.
