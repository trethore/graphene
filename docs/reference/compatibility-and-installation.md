# Compatibility and Installation

This page is the source of truth for supported Graphene artifacts.

## Compatibility

| Graphene | Loader | Minecraft | Java        | Fabric Loader   | Fabric API                                  |
|----------|--------|-----------|-------------|-----------------|---------------------------------------------|
| 2.0.0    | Fabric | 1.21.11   | 21 or newer | 0.19.3 or newer | 0.141.4+1.21.11 or newer compatible release |

Graphene's repository separates loader-independent code from loader- and Minecraft-specific modules. Future combinations
can be added as rows without changing the rest of the documentation.

## Maven Central

Maven Central is the canonical dependency source for mod development.

```kotlin
repositories {
  mavenCentral()
}

dependencies {
  modImplementation("io.github.trethore:graphene-ui:2.0.0")
}
```

Your Fabric project must also provide Fabric Loader and Fabric API versions compatible with the table above.

Declare the runtime dependency in `fabric.mod.json`:

```json
{
  "depends": {
    "grapheneui": ">=2.0.0"
  }
}
```

## Modrinth

Packaged releases are available from [Graphene on Modrinth](https://modrinth.com/mod/grapheneui). Select the file
matching the loader and Minecraft version in the compatibility table.

## GitHub Releases

[GitHub Releases](https://github.com/trethore/graphene/releases) publishes runtime JARs using this name:

```text
graphene-<loader>-<minecraft-version>-<graphene-version>.jar
```

For example:

```text
graphene-fabric-1.21.11-2.0.0.jar
```

Use Maven Central for compile-time dependency resolution. Modrinth and GitHub Releases are useful for launch profiles,
modpack distribution, and manual testing.

## Runtime installation

The Graphene mod includes the JCEF integration needed to install and launch its platform-specific browser runtime. By
default, native runtime files, browser cache, and logs are stored below:

```text
./graphene/browser-runtime/
```

The first startup on a platform can take longer while the runtime is prepared.

## Links

- [Maven Central artifact](https://central.sonatype.com/artifact/io.github.trethore/graphene-ui)
- [Modrinth project](https://modrinth.com/mod/grapheneui)
- [GitHub Releases](https://github.com/trethore/graphene/releases)
- [First web-screen tutorial](../tutorials/first-web-screen.md)
