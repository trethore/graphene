# Installation

Graphene is published as version-specific Fabric mod artifacts on Maven Central. Recommended integration is to depend
on the matching Graphene artifact as a separate runtime mod.

**Integration modes**

| Mode                   | Use when                       | Trade-off                                                 |
|------------------------|--------------------------------|-----------------------------------------------------------|
| Standalone runtime mod | Most mods                      | Users install the matching Graphene Fabric jar beside your mod |
| Jar-in-jar             | You need one distributable jar | Higher risk of version conflicts                          |

**Main API**

- `GrapheneCore.register(...)` - register a consumer during client init
- `GrapheneCore.handle(...)` - resolve the consumer-scoped handle
- `GrapheneConfig` - configure container and global runtime behavior

## Dependency Coordinates

- Group: `io.github.trethore`
- Artifact for Minecraft `1.21.11`: `graphene-ui-fabric-1.21.11`
- Version: `<version>`

Maven Central artifact path:

- `https://repo1.maven.org/maven2/io/github/trethore/graphene-ui-fabric-1.21.11/<version>/`

Releases:

- `https://github.com/trethore/graphene/releases`

## Gradle Setup

In your mod `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    modImplementation("io.github.trethore:graphene-ui-fabric-1.21.11:<version>")
}
```

In your `fabric.mod.json`, declare Graphene as a dependency. The Fabric mod id remains `graphene-ui` even though Maven
artifact ids are version-specific:

```json
{
  "depends": {
    "graphene-ui": ">=<version>"
  }
}
```

Runtime packaging model:

1. Ship your mod.
2. Install the matching `graphene-ui-fabric-<minecraft-version>-<version>.jar` in the same `mods/` directory.

## Optional Jar-In-Jar Model

If you need one distributable jar, you can embed Graphene:

```kotlin
dependencies {
    modImplementation("io.github.trethore:graphene-ui-fabric-1.21.11:<version>")
    include("io.github.trethore:graphene-ui-fabric-1.21.11:<version>")
}
```

Trade-offs:

- Version conflicts are more likely if multiple mods embed different Graphene versions.
- Users should not install both embedded and standalone Graphene copies.

## Register Your Mod

Call `GrapheneCore.register(...)` once during client init.
Later, access your scoped handle with `GrapheneCore.handle(MyModClient.class)`.

Prefer the anchor-class form when possible:

- `GrapheneCore.register(MyModClient.class)`
- `GrapheneCore.handle(MyModClient.class)`

If a project has an unusual source-set or entrypoint layout, you can register and resolve by explicit Fabric mod id
instead:

- `GrapheneCore.register("my-mod-id")`
- `GrapheneCore.handle("my-mod-id")`

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.register(MyModClient.class);
    }
}
```

If you register the same consumer multiple times, the config must be identical.
Registering the same consumer with different config throws `IllegalStateException`.

## Optional Runtime Config

`GrapheneConfig` is split into two parts:

- `GrapheneContainerConfig`: this mod's own configuration
- `GrapheneGlobalConfig`: this mod's contribution to the shared Graphene runtime

```java
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;
import tytoo.grapheneui.api.config.GrapheneRemoteDebugConfig;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneConfig config = GrapheneConfig.builder()
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
                                .allowedOrigins("*")
                                .build())
                        .allowFileSystemAccess()
                        .build())
                .build();

        GrapheneCore.register(MyModClient.class, config);
    }
}
```

Notes:

- `jcefDownloadPath(...)` is a base directory. Graphene installs under `<jcef-mvn-version>/<platform>`.
- If no consumer configures HTTP, `GrapheneRuntime.httpServer().isRunning()` is `false`.
- If remote debugging is not configured, `GrapheneRuntime.getRemoteDebuggingPort()` is `-1`.

## Shared Config Merge Rules

Graphene merges global contributions from all registered consumers before runtime initialization.

- `GrapheneGlobalConfig.jcefDownloadPath(...)`: all explicit values must match
- `GrapheneGlobalConfig.remoteDebugging(...)`: all explicit values must match
- `GrapheneGlobalConfig.extensionFolder(...)`: union merge across consumers
- `GrapheneGlobalConfig.fileSystemAccessMode(...)`: `ALLOW` wins if any consumer requests it

HTTP server settings are contributed per consumer through `GrapheneContainerConfig.http(...)`:

- `bindHost`, `baseUrlScheme`, and port selection must match across consumers using HTTP
- `fileRoot` and `spaFallback` stay private to each consumer mount

Conflicts throw a startup `IllegalStateException` that names both conflicting consumers.

## Compatibility Baseline

Current supported Fabric modules:

| Module            | Minecraft  | Fabric Loader | Fabric API             | Artifact                                                    |
|-------------------|------------|---------------|------------------------|-------------------------------------------------------------|
| `fabric-1.21.11` | `1.21.11` | `0.18.4`      | `0.141.3+1.21.11` | `io.github.trethore:graphene-ui-fabric-1.21.11:<version>` |

Java baseline: `21`.
