# Graphene

[![Loader: Fabric](https://img.shields.io/badge/Loader-Fabric-00BFA5?style=for-the-badge&logo=fabric)](https://modrinth.com/mod/fabric-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Minecraft: 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-5E8C31?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Modrinth](https://img.shields.io/badge/Modrinth-Graphene-1BD96A?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/grapheneui)

Graphene is a client-side UI library for Fabric Minecraft mods. It embeds Chromium through JCEF so mod developers
can render HTML/CSS/JavaScript UIs in-game while keeping a clean Java integration API.

![Graphene demo](docs/images/demo.png)

**At a glance**

| Area          | Details           |
|---------------|-------------------|
| Runtime       | Client-side       |
| UI engine     | Chromium via JCEF |
| Java baseline | `21`              |

**Supported versions**

| Module            | Minecraft  | Loader | Artifact                                                    |
|-------------------|------------|--------|-------------------------------------------------------------|
| `fabric-1.21.11` | `1.21.11` | Fabric | `io.github.trethore:graphene-ui-fabric-1.21.11:<version>` |

**Documentation**

- [Docs index](docs/README.md) - setup path and topic map
- [Installation](docs/installation.md) - dependency and runtime setup
- [Quickstart](docs/quickstart.md) - first web screen
- [Bridge](docs/bridge.md) - Java <-> JavaScript messaging

## What is Graphene?

Graphene bridges Minecraft modding and modern web UI development. Instead of writing every screen directly with
Minecraft rendering primitives, you can use browser capabilities while keeping your integration focused on Fabric and
the Minecraft version you target.

**Use Graphene to**

- build rich, responsive interfaces with familiar web technologies
- connect web interfaces to mod logic through Graphene's Java API
- iterate faster with standard web tooling and patterns
- avoid reinventing a full UI stack inside the game

## Requirements and Platforms

| Requirement | Value                           |
|-------------|---------------------------------|
| Java        | `21`                            |
| GPU         | NVIDIA GeForce GT 720 or better |
| macOS       | macOS 12 Monterey or later      |

| OS      | Architectures    |
|---------|------------------|
| macOS   | `arm64`, `amd64` |
| Linux   | `arm64`, `amd64` |
| Windows | `amd64`, `arm64` |

**Tested platforms**

| Platform      | Keyboard layouts                |
|---------------|---------------------------------|
| Windows 11    | `AZERTY`, `QWERTY`              |
| Linux Wayland | `AZERTY`, `QWERTY`              |
| macOS 26      | `QWERTY` (thanks to @Thinkseal) |

## Installation

Graphene is published as version-specific Fabric artifacts on Maven Central and GitHub Packages. Maven Central is
recommended because it does not require authentication.

**Release locations**

| Source          | Link                                                                                             |
|-----------------|--------------------------------------------------------------------------------------------------|
| Maven Central   | [io/github/trethore/graphene-ui-fabric-1.21.11](https://repo1.maven.org/maven2/io/github/trethore/graphene-ui-fabric-1.21.11/) |
| GitHub releases | [trethore/graphene releases](https://github.com/trethore/graphene/releases)                      |

### Maven Coordinates

```xml
<dependency>
  <groupId>io.github.trethore</groupId>
  <artifactId>graphene-ui-fabric-1.21.11</artifactId>
  <version>&lt;version&gt;</version>
</dependency>
```

### Add Graphene to a Fabric Gradle Project

Primary model: keep Graphene as a separate mod dependency.

| Model                | Recommendation | Notes                                                                       |
|----------------------|----------------|-----------------------------------------------------------------------------|
| Separate runtime mod | Recommended    | Install your mod jar and the matching Graphene Fabric jar in `mods/`        |
| Jar-in-jar           | Optional       | Higher risk of conflicts if multiple mods embed different Graphene versions |

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    modImplementation("io.github.trethore:graphene-ui-fabric-1.21.11:<version>")
}
```

Note: Graphene is also available on GitHub Packages.

In your `fabric.mod.json`, declare the Fabric mod id `graphene-ui`:

```json
{
  "depends": {
    "graphene-ui": ">=<version>"
  }
}
```

Jar-in-jar embedding is also possible. See [Installation](docs/installation.md) for trade-offs and setup.

### Initialize Graphene in Your Mod

Register your mod from `onInitializeClient()` with an anchor class. Graphene resolves the owning Fabric mod id from that
class, then later resolves the scoped `GrapheneHandle` from the same anchor class.

```java
import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.register(MyModClient.class);
    }
}
```

Graphene separates per-consumer container settings from shared runtime settings.

| Config area               | Purpose                                                                           |
|---------------------------|-----------------------------------------------------------------------------------|
| `GrapheneContainerConfig` | Per-consumer settings, such as HTTP mount behavior                                |
| `GrapheneGlobalConfig`    | Shared runtime contributions, such as JCEF path, extensions, and remote debugging |

`jcefDownloadPath(...)` is a base directory. Graphene installs JCEF under `<jcef-mvn-version>/<platform>`.

```java
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.GrapheneHandle;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;
import tytoo.grapheneui.api.config.GrapheneRemoteDebugConfig;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.register(
                MyModClient.class,
                GrapheneConfig.builder()
                        .container(GrapheneContainerConfig.builder()
                                .http(GrapheneHttpConfig.builder()
                                        .bindHost("127.0.0.1")
                                        .randomPortInRange(20_000, 21_000)
                                        .fileRoot(Path.of("C:/dev/my-ui-dist"))
                                        .spaFallback("/assets/my-mod-id/web/index.html")
                                        .build())
                                .build())
                        .global(GrapheneGlobalConfig.builder()
                                .jcefDownloadPath(Path.of("./graphene-jcef"))
                                .extensionFolder(Path.of("./config/my-mod/extensions"))
                                .remoteDebugging(GrapheneRemoteDebugConfig.builder()
                                        .randomPort()
                                        .allowedOrigins("https://chrome-devtools-frontend.appspot.com")
                                        .build())
                                .build())
                        .build()
        );
    }
}
```

Use the handle for namespaced helpers.

```java
GrapheneHandle graphene = GrapheneCore.handle(MyModClient.class);

String appUrl = graphene.appAssets().asset("web/index.html");
String mountedHttpUrl = graphene.httpUrl("web/index.html");
```

## Documentation

| Page                                       | Covers                            |
|--------------------------------------------|-----------------------------------|
| [Docs index](docs/README.md)               | Full reading order                |
| [Overview](docs/overview.md)               | Concepts and runtime model        |
| [Installation](docs/installation.md)       | Dependency and registration setup |
| [Quickstart](docs/quickstart.md)           | First screen integration          |
| [Troubleshooting](docs/troubleshooting.md) | Common failures and fixes         |

## Contributing

Contributions are welcome.

| Action                          | Link                                                        |
|---------------------------------|-------------------------------------------------------------|
| Read the contributor guide      | [CONTRIBUTING.md](CONTRIBUTING.md)                          |
| Report bugs or request features | [Issues](https://github.com/trethore/graphene/issues)       |
| Open changes                    | [Pull Requests](https://github.com/trethore/graphene/pulls) |

All pull requests must be tested before submission.

## License

Licensed under the [MIT License](LICENSE) by Titouan Réthoré.
