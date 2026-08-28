# Repository Guidelines

Graphene is a client-side UI library for Minecraft. Its goal is to provide a simple yet powerful
API for mod developers to create rich, web-based user interfaces in Minecraft using JCEF.

## Project Structure

Here is an overview of the project:

```text
graphene/
  .github/
  build-logic/                              # Included Gradle build for custom build logic.
    architecture-check/                     # Gradle plugin for enforcing architecture rules.
    sonar-analysis/                         # Gradle plugin for running SonarQube analysis.
  debug-client/                             # Development-only clients and resources for manually testing Graphene.
    <loader>-<minecraft-version>/           # Loader and Minecraft-version-specific debug client.
      src/main/java/io/github/trethore/graphene/debug/
      src/main/resources/
      build.gradle.kts
    shared/resources/                       # Test pages, scripts, styles, translations, and assets shared by debug clients.
  docs/
  packages/
    common/                                 # Loader-independent Graphene API, runtime, JCEF integration, and web resources.
      src/main/
        java/io/github/trethore/graphene/
          api/                              # Public browser, bridge, configuration, runtime, and URL APIs.
          internal/                         # Shared runtime, JCEF, bridge, HTTP, platform, and resource internals.
        resources/assets/grapheneui/
      src/test/
      build.gradle.kts
    <loader>-<minecraft-version>/           # Loader and Minecraft-version-specific implementation.
      src/main/
        java/io/github/trethore/graphene/
          fabric/                           # Fabric-specific public APIs and internal integrations.
          mixin/
          FabricBootstrap.java
        resources/
          assets/grapheneui/
          fabric.mod.json
          grapheneui.mixins.json
      src/test/
      build.gradle.kts
  .gitignore
  build.gradle.kts
  CHANGELOG.md
  gradle.properties
  LICENSE
  README.md
  settings.gradle.kts
```

Graphene supports `fabric-1.21.11` and `fabric-26.2`. Read `settings.gradle.kts` for more information.

## General Coding Conventions

- `packages/common` should contain only the version-independent logic that is shared across all Minecraft implementations.
- `packages/<loader>-<minecraft-version>` should contain version-dependent code, like the mod entry point, integration logic, mixins, and Minecraft/loader dependencies.
- Do not write comments unless documentation is explicitly requested by the user.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.

## Java Expectations

- Prefer explicit types over `var`, and use descriptive names instead of one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors, overridden methods,
  public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.

## Testing & Verification

- Run `./gradlew check --quiet` to catch Java compilation errors, formatting issues, and execute tests.
- Run `./gradlew spotlessApply --quiet` to format changes directly instead of running `./gradlew spotlessCheck` first and then fixing formatting issues.
- Do not run long-running Gradle tasks, such as game launches. Instead, provide the exact command for the user to run, for example:
  `./gradlew :packages:fabric-1.21.11:runClient`

## Dependencies and External Source Browsing

- Assume that JDK tools such as `javap`, `jdeps`, and `javadoc`, as well as `cfr`, are available.
- Read `gradle/libs.versions.toml` to identify the dependencies and versions used by the project.
- Look in `~/.gradle/caches/modules-2/files-2.1/` to locate the downloaded dependencies.

## Commits & Pull Requests

- Follow the Conventional Commits specification for commit messages.
- Pull request summaries should include the related issue(s), a brief description of the changes, and how the changes were tested.
