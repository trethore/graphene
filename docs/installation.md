# Installation

Graphene is published on Maven Central and released as a ready-to-use remapped Fabric mod jar.
The primary integration model is to depend on Graphene as a separate mod in `mods/`.

## 1) Get the latest version

Use this URL pattern for a specific release:

- `https://repo1.maven.org/maven2/io/github/trethore/graphene-ui/<version>/`

Release artifacts (remapped jar + sources jar) are also published here:

- https://github.com/trethore/graphene/releases

Dependency coordinates:

- Group: `io.github.trethore`
- Artifact: `graphene-ui`
- Version: `<version>`

## 2) Primary setup: separate Graphene mod dependency (recommended)

In your mod `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    modImplementation("io.github.trethore:graphene-ui:<version>")
}
```

Note: Graphene is also published on GitHub Packages if you need that distribution path.

In your mod `fabric.mod.json`, declare Graphene as a runtime dependency:

```json
{
  "depends": {
    "graphene-ui": ">=<version>"
  }
}
```

Runtime distribution for this model:

1. Ship your mod jar.
2. Install `graphene-ui-<version>.jar` in the same `mods/` folder.

This is the best option for compatibility across modpacks and avoids bundling duplicate Graphene copies.

## 3) Alternative setup: jar-in-jar (possible, but not preferred)

If you want a single distributable mod jar, you can embed Graphene with jar-in-jar:

```kotlin
dependencies {
    modImplementation("io.github.trethore:graphene-ui:<version>")
    include("io.github.trethore:graphene-ui:<version>")
}
```

Important trade-offs:

- If multiple mods embed different Graphene versions, dependency resolution can fail.
- Users should not additionally install a standalone `graphene-ui` jar when already embedding one.
- Prefer the recommended separate-mod model unless you explicitly need single-jar distribution.

## 4) Initialize Graphene in your client entrypoint

Register your mod with `GrapheneCore.register("my-mod-id")` in your `ClientModInitializer`:

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.register("my-mod-id");
    }
}
```

Optional: pass a `GrapheneConfig` if you want a custom JCEF download directory, to load unpacked extensions,
or to enable remote debugging.
`jcefDownloadPath(...)` is treated as a base directory, and Graphene installs JCEF under
`<jcef-mvn-version>/<platform>`:

```java
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneConfig;
import tytoo.grapheneui.api.GrapheneCore;
import tytoo.grapheneui.api.GrapheneRemoteDebugConfig;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneConfig config = GrapheneConfig.builder()
                .jcefDownloadPath(Path.of("./graphene-jcef"))
                .extensionFolder(Path.of("./config/my-mod/extensions"))
                .remoteDebugging(GrapheneRemoteDebugConfig.builder().randomPort().build())
                .build();

        GrapheneCore.register("my-mod-id", config);
    }
}
```

If this is missing, Graphene startup fails when client loading finishes, because no consumer/config was registered for merge.

## 5) Compatibility baseline

Current Graphene baseline:

- Minecraft: `1.21.11`
- Fabric Loader: `0.18.4`
- Fabric API: `0.141.3+1.21.11`
- Java: `21`
- GPU: `NVIDIA GeForce GT 720` or better
- For mac users, macOS 12 (Monterey) or later.

## 6) Supported platforms

- macOS: `arm64`, `amd64`
- Linux: `arm64`, `amd64`
- Windows: `amd64`

## 7) Tested platforms

- Windows 11 with `AZERTY` and `QWERTY` keyboard layouts
- Linux (Wayland) with `AZERTY` and `QWERTY` keyboard layouts
- MacOS 26 with `QWERTY` keyboard layout. (Thx to @Thinkseal for testing on macOS!)

## 8) Quick verification

After wiring the dependency and initializer:

1. Run your client and confirm startup succeeds.
2. Open a screen containing a `GrapheneWebViewWidget`.
3. Confirm the page renders and no `Graphene is not initialized` error appears.

---

Next: [Quickstart](quickstart.md)
