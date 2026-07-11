# Source References

Unpack selected dependency sources into `references/`:

```sh
./gradlew unpackSources
```

Clean generated references:

```sh
./gradlew cleanUnpackedSources
```

Selected libraries are declared through the `unpack` configuration. Dependencies declared by multiple modules are
deduplicated and unpacked once by the root task. Source jars are used when available; otherwise the binary jar is
decompiled with CFR.

Reference directories use the complete module coordinate, for example:

```text
references/net.fabricmc.fabric-api-fabric-api-0.141.4-1.21.11/
```

Enable `unpackNestedJars` to decompile nested jars. Fabric archives use the paths declared by `fabric.mod.json`; regular
archives fall back to discovering embedded `.jar` files. Nested jars are processed recursively, and a path hash is
included in each output directory to prevent filename collisions.

```kotlin
import io.github.trethore.buildlogic.unpack

val minecraftVersion = "1.21.11"

dependencies {
    unpack(minecraft("com.mojang:minecraft:$minecraftVersion"))
}
```

Dependencies must declare concrete versions. Dynamic selectors such as `1.+`, `latest.release`, and version ranges are
rejected so generated references remain reproducible.

Reference options can be declared from the root build file:

```kotlin
references {
    unpackNestedJars = true

    git(
        url = "https://github.com/FabricMC/fabric.git",
        branch = "main",
        commit = null,
    )
}
```
