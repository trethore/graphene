# Repository Guidelines

Graphene is a client-side UI library for Minecraft. Its goal is to provide a simple yet powerful
API for mod developers to create rich, web-based user interfaces in Minecraft using JCEF.

## Project Structure

Here is an overview of the project:

```text
graphene/                                   # You are here!
  .github/                                  # GitHub config and workflows.
  build-logic/                              # Included Gradle build for custom build logic.
    sonar/                                  # Gradle plugin for running SonarQube analysis.
    unpack-sources/                         # Gradle plugin that unpacks dependency and Git reference sources.
  docs/
  packages/
    common/                                 # Shared mod logic with no Minecraft or Fabric dependencies.
      src/main/java/io/github/trethore/graphene/
        api/                                # Public entry points used by loader/version implementations.
          Main.java
        internal/                           # Private common implementation details.
      build.gradle.kts
    fabric-1.21.11/                         # Fabric implementation for Minecraft 1.21.11.
      src/main/
        java/io/github/trethore/graphene/
          mixin/                            # Minecraft/Fabric-version-specific mixins.
          FabricBootstrap.java              # Fabric ModInitializer that boots common code.
        resources/
          assets/grapheneui/                 # Fabric mod assets.
          grapheneui.mixins.json
          fabric.mod.json
      build.gradle.kts
  references/                               # Dependency source code for browsing and reference.
    net.fabricmc.fabric-api-fabric-api-0.141.4-1.21.11/
      nested/                               # Source code of the nested jars.
    com.mojang-minecraft-1.21.11/
    <group>-<lib-name>-<version>/
  .gitignore
  build.gradle.kts                          # Root Gradle config shared by all projects
  gradle.properties                         # Shared version and dependency properties.
  README.md
  settings.gradle.kts
```

## General Coding Conventions

- `packages/common` should contain only the version-independent logic that is shared across all Minecraft implementations.
- `packages/<loader>-<version>` should contain version-dependent code, like the mod entry point, integration logic, mixins, and Minecraft/loader dependencies.
- Avoid comments unless documentation is explicitly requested.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.

## Java Expectations

- Prefer explicit types over `var`, and use descriptive names instead of one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors, overridden methods,
  public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.

## Testing & Verification

- Run `./gradlew check` to catch Java compilation errors, formatting issues, and execute tests.
- Do not run long-running Gradle tasks, such as game launches. Instead, provide the exact command for the user to run, for example:
  `./gradlew :packages:fabric-1.21.11:runClient`

## Dependencies & External Sources

- Library source code is available in the `references` directory for browsing and reference only. Do not edit it.
- The `references` directory is generated via the `./gradlew unpackSources` command.
- You can clean the generated references by running `./gradlew cleanUnpackedSources`.

## Pull Requests & Commits

- Pull request summaries should include the related issue(s), a brief description of the changes, and how the changes were tested.
- Follow the Conventional Commits specification for commit messages.
