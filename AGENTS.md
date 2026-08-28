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
    graphene-conventions/                   # Gradle conventions that compose shared sources into concrete targets.
    sonar-analysis/                         # Gradle plugin for running SonarQube analysis.
  config/                                   # Qodana and SonarQube configuration.
  debug-client/                             # Development-only clients and resources for manually testing Graphene.
    fabric-shared/                          # Fabric debug entry point and key bindings shared across Fabric targets.
      src/main/java/io/github/trethore/graphene/debug/
    shared/                                 # Version-shared debug UI, bridge logic, and web resources for Fabric clients.
      src/main/java/io/github/trethore/graphene/debug/
      resources/assets/grapheneui-debug/
    <loader>-<minecraft-version>/           # Target-specific debug access, metadata, and Gradle configuration.
      src/main/java/io/github/trethore/graphene/debug/
      src/main/resources/
      build.gradle.kts
  docs/
  gradle/libs.versions.toml
  packages/
    common/                                 # Minecraft- and loader-independent API, runtime, JCEF integration, and web resources.
      src/main/
        java/io/github/trethore/graphene/
          api/                              # Public browser, bridge, configuration, runtime, and URL APIs.
          internal/                         # Shared runtime, JCEF, bridge, HTTP, platform, and resource internals.
        resources/assets/grapheneui/
      src/test/
      build.gradle.kts
    fabric-shared/                          # Fabric and Minecraft integrations shared across Fabric targets.
      src/main/java/io/github/trethore/graphene/
        fabric/                             # Shared Fabric public APIs and internal integrations.
        mixin/
        FabricBootstrap.java
      src/test/java/io/github/trethore/graphene/fabric/
    minecraft-shared/                       # Minecraft-dependent, loader-independent code shared by compatible targets.
      src/main/java/io/github/trethore/graphene/minecraft/
      src/test/java/io/github/trethore/graphene/minecraft/
    <loader>-<minecraft-version>/           # Code, resources, and build configuration specific to one target.
      src/main/
        java/io/github/trethore/graphene/
          fabric/                           # Target-specific Fabric APIs and integrations.
          minecraft/                        # Target-specific Minecraft compatibility code.
          mixin/
        resources/
          assets/grapheneui/
          fabric.mod.json
          grapheneui.mixins.json
      build.gradle.kts
  scripts/release/                          # Release automation scripts.
  .gitignore
  build.gradle.kts
  CHANGELOG.md
  gradle.properties
  LICENSE
  README.md
  settings.gradle.kts
```

Graphene supports `fabric-1.21.11` and `fabric-26.2`. Read the related `build.gradle.kts` and `settings.gradle.kts` for more information.
The shared package and debug-client directories are composed into concrete target source sets by
`build-logic/graphene-conventions`; they are not standalone Gradle projects.

## General Coding Conventions

- `packages/common` must remain independent of Minecraft, Fabric, Mojang rendering, and LWJGL. Put shared public APIs, runtime logic, JCEF integration, and platform abstractions here.
- `packages/minecraft-shared` may depend on Minecraft but must not depend on a mod loader. Code in this directory must compile unchanged for every target that includes it.
- `packages/fabric-shared` may depend on Minecraft and Fabric. Put Fabric-specific code here when it is compatible with all supported Fabric targets.
- `packages/<loader>-<minecraft-version>` should contain only code, resources, and build configuration that differ for that target. Move code to the narrowest valid shared layer instead of duplicating it between targets.
- Apply the equivalent placement rules under `debug-client`: use `shared` for target-shared debug behavior and resources, `fabric-shared` for shared Fabric loader integration, and concrete target directories for version-specific adapters.
- Avoid redundant comments. Add or update Javadocs for public APIs, and use implementation comments only to explain non-obvious constraints or behavior.
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
