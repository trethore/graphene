# Repository Guidelines

Graphene is a client-side UI library for Minecraft. Its goal is to provide a simple yet powerful
API for mod developers to create rich, web-based user interfaces in Minecraft using JCEF.

## Project Structure

Here is an overview of the project:

```text
graphene/                                   # You are here!
  .github/                                  # GitHub config and workflows.
  build-logic/                              # Included Gradle build for custom build logic.
    architecture-check/                     # Gradle plugin for enforcing architecture rules.
    sonar-analysis/                         # Gradle plugin for running SonarQube analysis.
    unpack-sources/                         # Gradle plugin that unpacks dependency and Git reference sources.
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
        resources/assets/grapheneui/        # JavaScript resources injected into Graphene browser sessions.
      src/test/                             # Unit tests and test resources for common functionality.
      build.gradle.kts
    <loader>-<minecraft-version>/           # Loader and Minecraft-version-specific implementation.
      src/main/
        java/io/github/trethore/graphene/
          fabric/                           # Fabric-specific public APIs and internal integrations.
          mixin/                            # Minecraft/Fabric-version-specific mixins.
          FabricBootstrap.java              # Fabric ModInitializer that boots common code.
        resources/
          assets/grapheneui/                # Fabric mod assets.
          fabric.mod.json
          grapheneui.mixins.json
      src/test/                             # Unit tests for Fabric-specific functionality.
      build.gradle.kts
  references/                               # Dependency source code for browsing and reference.
    <group>-<lib-name>-<version>/
    com.mojang-minecraft-1.21.11/
    net.fabricmc.fabric-api-fabric-api-0.141.4-1.21.11/
      nested/                               # Source code of the nested jars.
  .gitignore
  build.gradle.kts                          # Root Gradle config shared by all projects
  CHANGELOG.md
  gradle.properties                         # Shared version and dependency properties.
  README.md
  settings.gradle.kts
```

Graphene supports `fabric-1.21.11` and `fabric-26.2`. See `settings.gradle.kts` for more information.

## General Coding Conventions

- `packages/common` should contain only the version-independent logic that is shared across all Minecraft implementations.
- `packages/<loader>-<minecraft-version>` should contain version-dependent code, like the mod entry point, integration logic, mixins, and Minecraft/loader dependencies.
- Avoid comments unless documentation is explicitly requested.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.

## Java Expectations

- Prefer explicit types over `var`, and use descriptive names instead of one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors, overridden methods,
  public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.

## Testing & Verification

- Run `./gradlew check` to catch Java compilation errors, formatting issues, and execute tests.
- Run `./gradlew spotlessApply` to format changes directly instead of running `./gradlew spotlessCheck` first and then fixing formatting issues.
- Do not run long-running Gradle tasks, such as game launches. Instead, provide the exact command for the user to run, for example:
  `./gradlew :packages:fabric-1.21.11:runClient`

## Dependencies & External Sources

- Library source code is available in the `references` directory for browsing and reference only. Do not edit it.
- The `references` directory is generated via the `./gradlew unpackSources` command.
- You can clean the generated references by running `./gradlew cleanUnpackedSources`.

## Pull Requests & Commits

- Pull request summaries should include the related issue(s), a brief description of the changes, and how the changes were tested.
- Follow the Conventional Commits specification for commit messages.
