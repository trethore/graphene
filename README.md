# Graphene

Graphene is a client-side UI library for Minecraft 1.21.11 (Fabric) that lets mod developers build interfaces with web technologies.
It embeds Chromium through JCEF, so you can render HTML/CSS/JavaScript UIs in-game while keeping a clean Java API for mod integration.

![Graphene demo](docs/images/demo.png)

## What is this ?

Graphene is meant to bridge Minecraft modding and modern web UI development.

Instead of writing every screen directly with Minecraft rendering primitives, you can:

- build rich, responsive interfaces using browser capabilities;
- connect those interfaces to your mod logic through Graphene's API;
- iterate on UI faster with familiar web tooling and patterns;
- keep the integration focused on Fabric + Minecraft 1.21.11.

In short: Graphene gives Fabric mods a practical way to use web-powered interfaces without reinventing a full UI stack inside the game.

## Requirements

- Java: `21`
- GPU: `NVIDIA GeForce GT 720` or better
- For mac users, macOS 12 (Monterey) or later.

## Supported Platforms

- macOS: `arm64`, `amd64`
- Linux: `arm64`, `amd64`
- Windows: `amd64`

## Tested Platforms

- Windows 11 with `AZERTY` and `QWERTY` keyboard layouts
- Linux (Wayland) with `AZERTY` and `QWERTY` keyboard layouts
- MacOS 26 with `QWERTY` keyboard layout. (Thx to @Thinkseal for testing on macOS!)

## Installation

Graphene is published on GitHub Packages. Check the latest available version here:
[https://github.com/trethore/graphene/packages](https://github.com/trethore/graphene/packages)

### Maven coordinates

```xml
<dependency>
  <groupId>tytoo.grapheneui</groupId>
  <artifactId>graphene-ui</artifactId>
  <version>&lt;version&gt;</version>
</dependency>
```

### Add Graphene to a Fabric Minecraft Gradle project

Primary model (recommended): keep Graphene as a separate mod dependency.

```kotlin
repositories {
    maven {
        name = "GitHubPackagesGraphene"
        url = uri("https://maven.pkg.github.com/trethore/graphene")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    modImplementation("tytoo.grapheneui:graphene-ui:<version>")
}
```

In your `fabric.mod.json`, declare:

```json
{
  "depends": {
    "graphene-ui": ">=<version>"
  }
}
```

At runtime, place both jars in `mods/`: your mod jar and `graphene-ui-<version>.jar`.

Jar-in-jar embedding is also possible, but it is not the preferred default. See [docs/installation.md](docs/installation.md) for the trade-offs and setup.

### Initialize Graphene in your mod

Register your mod with `GrapheneCore.init("your-mod-id")` from your client initializer:

```java
import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.init("my-mod-id");
    }
}
```

If you need shared runtime options (HTTP, JCEF path, extension folders), pass a `GrapheneConfig`.
`jcefDownloadPath(...)` is a base directory, and Graphene installs JCEF under `<jcef-mvn-version>/<platform>`:

```java
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneConfig;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneConfig config = GrapheneConfig.builder()
                .jcefDownloadPath(Path.of("./graphene-jcef"))
                .extensionFolder(Path.of("./config/my-mod/extensions"))
                .build();

        GrapheneCore.init("my-mod-id", config);
    }
}
```

## Documentation

Start [HERE](docs/README.md)!

## Contributing

Contributions are welcome!

- Report bugs or request features in [Issues](https://github.com/trethore/graphene/issues).
- Open changes through [Pull Requests](https://github.com/trethore/graphene/pulls).
- All pull requests must be tested before being submitted.

## License

Licensed under the [MIT License](LICENSE) by Titouan Réthoré.
