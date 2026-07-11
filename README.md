# Example Mod

This is just an example mod.

## Project Structure

Here is an overview of the project:

```text
example-mod/
├── build-logic/                            # Included Gradle build for custom build logic.
│   ├── sonar/                              # Gradle plugin for running SonarQube analysis.
│   └── unpack-sources/                     # Gradle plugin that unpacks dependency and Git reference sources.
├── docs/                                   # Project setup, running, and source reference documentation.
├── packages/
│   ├── common/                             # Shared mod logic with no Minecraft or Fabric dependencies.
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/example/
│   │       ├── api/                        # Public entry points used by loader/version implementations.
│   │       │   └── Main.java
│   │       └── internal/                   # Private common implementation details.
│   └── fabric-1.21.11/                     # Fabric implementation for Minecraft 1.21.11.
│       ├── build.gradle.kts
│       └── src/main/
│           ├── java/com/example/
│           │   ├── FabricBootstrap.java    # Fabric ModInitializer that boots common code.
│           │   └── mixin/                  # Minecraft/Fabric-version-specific mixins.
│           └── resources/
│               ├── assets/example-mod/     # Fabric mod assets.
│               ├── example-mod.mixins.json
│               └── fabric.mod.json
├── .github/                                # GitHub config and workflows.
├── .gitignore
├── build.gradle.kts                        # Root Gradle config shared by all projects.
├── gradle.properties                       # Shared version and dependency properties.
├── settings.gradle.kts
└── README.md
```

- `packages/common` contains the version-independent logic and entry point (`com.example.api.Main`).
- `packages/fabric-1.21.11` contains the version-dependent Fabric `ModInitializer`, integration logic, mixins,
  resources, and Minecraft/Fabric dependencies.

## Documentation

Start [HERE](docs/README.md)!

## License

Licensed under the [MIT License](LICENSE) by Titouan Réthoré.
