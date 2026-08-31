# Install Graphene

Add the Graphene artifact that matches your Minecraft version, then declare its Fabric runtime dependency.

## Check compatibility

Confirm the required Java, Fabric Loader, and Fabric API versions in the
[compatibility reference](../reference/compatibility-and-installation.md).

## Add the Maven dependency

Graphene is published on Maven Central:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.trethore:graphene-ui-26.2:2.4.1")
    // For Minecraft 26.1.2 instead:
    // implementation("io.github.trethore:graphene-ui-26.1.2:2.4.1")
    // For Minecraft 1.21.11 instead:
    // modImplementation("io.github.trethore:graphene-ui-1.21.11:2.4.1")
}
```

Select one artifact only. Your project must also provide the Fabric Loader and Fabric API versions listed in the
compatibility reference.

## Declare the Fabric dependency

Add Graphene to your `fabric.mod.json` dependencies so Fabric loads it before your mod uses the API:

```json
{
  "depends": {
    "grapheneui": ">=2.4.1"
  }
}
```

## Verify the installation

Start a development client and confirm that Fabric lists `grapheneui` as a loaded mod. The first startup can take
longer while Graphene prepares its platform-specific JCEF runtime.

Do not create a browser as the installation check. First register your mod and let runtime initialization complete.
Browser sessions can be created only after Graphene reaches the `RUNNING` state; see
[Architecture and runtime](../explanation/architecture-and-runtime.md).

## Install a packaged release manually

For a launch profile or modpack, use a JAR from Modrinth or GitHub Releases that matches the loader and Minecraft
version in the compatibility table. Maven Central remains the dependency source for mod development.

## Next steps

- [Build your first web screen](../tutorials/first-web-screen.md).
- [Look up artifact names and runtime paths](../reference/compatibility-and-installation.md).
