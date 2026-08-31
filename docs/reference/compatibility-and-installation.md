# Compatibility and Artifacts

This page is the source of truth for supported Graphene targets, artifact coordinates, and runtime file locations. For
installation steps, use [Install Graphene](../how-to/install-graphene.md).

## Compatibility

| Graphene | Loader | Minecraft | Java        | Fabric Loader   | Fabric API                                  |
|----------|--------|-----------|-------------|-----------------|---------------------------------------------|
| 2.4.1    | Fabric | 26.2      | 25 or newer | 0.19.3 or newer | 0.158.0+26.2 or newer compatible release    |
| 2.4.1    | Fabric | 26.1.2    | 25 or newer | 0.19.3 or newer | 0.155.2+26.1.2 or newer compatible release  |
| 2.4.1    | Fabric | 1.21.11   | 21 or newer | 0.19.3 or newer | 0.141.6+1.21.11 or newer compatible release |

Graphene's repository separates loader-independent code from loader- and Minecraft-specific modules.

## Maven coordinates

| Minecraft | Maven coordinate                                      |
|-----------|-------------------------------------------------------|
| 26.2      | `io.github.trethore:graphene-ui-26.2:2.4.1`           |
| 26.1.2    | `io.github.trethore:graphene-ui-26.1.2:2.4.1`         |
| 1.21.11   | `io.github.trethore:graphene-ui-1.21.11:2.4.1`        |

The Fabric mod ID is `grapheneui`.

## Packaged releases

Packaged releases are available from [Graphene on Modrinth](https://modrinth.com/mod/grapheneui). Select the file
matching the loader and Minecraft version in the compatibility table.

[GitHub Releases](https://github.com/trethore/graphene/releases) publishes runtime JARs using this name:

```text
graphene-<graphene-version>-<loader>-<minecraft-version>.jar
```

For example:

```text
graphene-2.4.1-fabric-26.2.jar
graphene-2.4.1-fabric-26.1.2.jar
graphene-2.4.1-fabric-1.21.11.jar
```

Maven Central is the dependency source for mod development. Modrinth and GitHub Releases provide runtime JARs for
launch profiles, modpack distribution, and manual testing.

## Runtime installation

The Graphene mod includes the JCEF integration needed to install and launch its platform-specific browser runtime. The
default base directory is:

```text
./graphene/browser-runtime/
```

Graphene adds the JCEF version and platform identifier below this base directory:

```text
./graphene/browser-runtime/<jcef-version>/<platform>/
```

The browser cache is in `cache/` below that directory. The Chromium/JCEF log is `logs.txt` in the same directory.

## Links

- [Maven Central artifact for Minecraft 26.2](https://central.sonatype.com/artifact/io.github.trethore/graphene-ui-26.2)
- [Maven Central artifact for Minecraft 26.1.2](https://central.sonatype.com/artifact/io.github.trethore/graphene-ui-26.1.2)
- [Maven Central artifact for Minecraft 1.21.11](https://central.sonatype.com/artifact/io.github.trethore/graphene-ui-1.21.11)
- [Modrinth project](https://modrinth.com/mod/grapheneui)
- [GitHub Releases](https://github.com/trethore/graphene/releases)
- [Installation guide](../how-to/install-graphene.md)
- [First web-screen tutorial](../tutorials/first-web-screen.md)
