# Installation

Graphene is published as a Fabric mod on Maven Central.
Recommended integration is to depend on Graphene as a separate runtime mod.

## 1) Dependency Coordinates

- Group: `io.github.trethore`
- Artifact: `graphene-ui`
- Version: `<version>`

Maven Central artifact path:

- `https://repo1.maven.org/maven2/io/github/trethore/graphene-ui/<version>/`

Releases:

- `https://github.com/trethore/graphene/releases`

## 2) Gradle Setup (Recommended)

In your mod `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    modImplementation("io.github.trethore:graphene-ui:<version>")
}
```

In your `fabric.mod.json`, declare Graphene as a dependency:

```json
{
  "depends": {
    "graphene-ui": ">=<version>"
  }
}
```

Runtime packaging model:

1. Ship your mod.
2. Install `graphene-ui-<version>.jar` in the same `mods/` directory.

## 3) Optional Jar-In-Jar Model

If you need one distributable jar, you can embed Graphene:

```kotlin
dependencies {
    modImplementation("io.github.trethore:graphene-ui:<version>")
    include("io.github.trethore:graphene-ui:<version>")
}
```

Trade-offs:

- Version conflicts are more likely if multiple mods embed different Graphene versions.
- Users should not install both embedded and standalone Graphene copies.

## 4) Register Your Mod

Call `GrapheneCore.register(...)` once during client init.
Later, access your scoped handle with `GrapheneCore.handle(MyModClient.class)`.

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

## 5) Optional Runtime Config

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
                                .allowedOrigins("https://chrome-devtools-frontend.appspot.com")
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

## 6) Shared Config Merge Rules

Graphene merges global contributions from all registered consumers before runtime initialization.

- `GrapheneGlobalConfig.jcefDownloadPath(...)`: all explicit values must match
- `GrapheneGlobalConfig.remoteDebugging(...)`: all explicit values must match
- `GrapheneGlobalConfig.extensionFolder(...)`: union merge across consumers
- `GrapheneGlobalConfig.fileSystemAccessMode(...)`: `ALLOW` wins if any consumer requests it

HTTP server settings are contributed per consumer through `GrapheneContainerConfig.http(...)`:

- `bindHost`, `baseUrlScheme`, and port selection must match across consumers using HTTP
- `fileRoot` and `spaFallback` stay private to each consumer mount

Conflicts throw a startup `IllegalStateException` that names both conflicting consumers.

## 7) Compatibility Baseline

Current repository baseline:

- Minecraft `1.21.11`
- Fabric Loader `0.18.4`
- Fabric API `0.141.3+1.21.11`
- Java `21`

---

Next: [Quickstart](quickstart.md)
