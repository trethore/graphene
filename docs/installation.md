# Installation

Graphene is published on GitHub Packages and consumed as a normal Fabric mod dependency.

## 1) Get the latest version

Check the package page for the newest release:

- https://github.com/trethore/graphene/packages

Dependency coordinates:

- Group: `tytoo.grapheneui`
- Artifact: `graphene-ui`
- Version: `<version>`

## 2) Configure GitHub Packages access

GitHub Packages Maven downloads require credentials. The simplest setup is to define them in your user Gradle properties (`~/.gradle/gradle.properties`):

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Your token should have `read:packages` permission.

## 3) Add the repository and dependency (Fabric Loom)

In your mod `build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "GitHubPackagesGraphene"
        url = uri("https://maven.pkg.github.com/trethore/graphene")
        credentials {
            username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
            password = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    modImplementation("tytoo.grapheneui:graphene-ui:<version>")
}
```

Use `modImplementation` (not plain `implementation`) so Fabric Loom treats Graphene as a mod dependency.

## 4) Initialize Graphene in your client entrypoint

Call `GrapheneCore.init()` once in your `ClientModInitializer`:

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import tytoo.grapheneui.api.GrapheneCore;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GrapheneCore.init();
    }
}
```

Optional: pass a `GrapheneConfig` if you want a custom JCEF download directory or to load unpacked extensions.
`jcefDownloadPath(...)` is treated as a base directory, and Graphene installs JCEF under
`<jcef-mvn-version>/<platform>`:

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

        GrapheneCore.init(config);
    }
}
```

If this is missing, runtime calls that need Graphene will fail with an initialization error.

## 5) Compatibility baseline

Current Graphene baseline:

- Minecraft: `1.21.11`
- Fabric Loader: `0.18.4`
- Fabric API: `0.141.3+1.21.11`
- Java: `21`
- GPU: `NVIDIA GeForce GT 720` or better

## 6) Supported platforms

- macOS: `arm64`, `amd64`
- Linux: `arm64`, `amd64`
- Windows: `amd64`

## 7) Tested platforms

- Windows 11 with `AZERTY` and `QWERTY` keyboard layouts
- Linux (Wayland) with `AZERTY` and `QWERTY` keyboard layouts

## 8) Quick verification

After wiring the dependency and initializer:

1. Run your client and confirm startup succeeds.
2. Open a screen containing a `GrapheneWebViewWidget`.
3. Confirm the page renders and no `Graphene is not initialized` error appears.

---
Next: [Quickstart](quickstart.md)
