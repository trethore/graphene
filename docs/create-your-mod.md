# Turn the Example into Your Mod

Follow this tutorial to replace the example identity with your mod's name, ID, Java package, metadata, and publishing
coordinates. Use IntelliJ IDEA's refactoring tools for Java packages so package declarations, imports, and directories
stay synchronized.

## The five-minute version

Choose these values before editing:

| Meaning                        | Example value               | Current value |
|--------------------------------|-----------------------------|---------------|
| Display name                   | `Inventory Helpers`         | `Example Mod` |
| Mod ID and Gradle project name | `inventory-helpers`         | `example-mod` |
| Java package                   | `dev.alex.inventoryhelpers` | `com.example` |
| Maven group                    | `dev.alex`                  | `com.example` |

The mod ID is the stable machine-readable identity used by Fabric, resources, logs, and generated artifact names.
Changing it after publishing creates migration work, so choose it carefully.

### 1. Rename the Java package

In IntelliJ IDEA, use **Refactor -> Rename** on `com.example` in each source root:

- `packages/common/src/main/java/com/example`
- `packages/fabric-1.21.11/src/main/java/com/example`

Rename it to your package, such as `dev.alex.inventoryhelpers`. Let IntelliJ update package declarations and imports,
and confirm that it also moves the files into matching directories.

This updates the references in:

- `Main.java`
- `package-info.java`
- `FabricBootstrap.java`
- `ExampleMixin.java`

Then update the two resource references that Java refactoring may not detect:

- `packages/fabric-1.21.11/src/main/resources/fabric.mod.json`
    - `com.example.FabricBootstrap` -> your package plus `.FabricBootstrap`
- `packages/fabric-1.21.11/src/main/resources/example-mod.mixins.json`
    - `com.example.mixin` -> your package plus `.mixin`

### 2. Rename the mod ID

Replace `example-mod` with your mod ID in these places:

- `settings.gradle.kts`: `rootProject.name`
- `packages/fabric-1.21.11/src/main/java/.../FabricBootstrap.java`: `MOD_ID`
- `packages/fabric-1.21.11/src/main/resources/fabric.mod.json`:
    - `id`
    - the namespace in `icon`
    - the mixin configuration filename in `mixins`

Rename these resource paths to match:

```text
packages/fabric-1.21.11/src/main/resources/example-mod.mixins.json
  -> packages/fabric-1.21.11/src/main/resources/<mod-id>.mixins.json

packages/fabric-1.21.11/src/main/resources/assets/example-mod/
  -> packages/fabric-1.21.11/src/main/resources/assets/<mod-id>/
```

The repository directory itself can also be renamed, but Gradle does not require it to match the mod ID.

### 3. Update the public metadata

Edit `packages/fabric-1.21.11/src/main/resources/fabric.mod.json`:

- `name`
- `description`
- `authors`
- `contact.sources`
- `license`, if you are not keeping MIT
- `icon`, if the filename or namespace changed

Replace `packages/fabric-1.21.11/src/main/resources/assets/<mod-id>/icon.png` with your icon.

If the license or copyright holder changes, update `LICENSE` and the license section in the root `README.md`.

### 4. Set the Maven coordinates and version

Edit `gradle.properties`:

```properties
mod_version=0.1.0
maven_group=dev.alex
```

The Maven group identifies published Java artifacts. It commonly matches the organization-owned prefix of the Java
package, but it does not need to include the mod name.

### 5. Verify the result

Search for template values that should no longer describe your mod:

```bash
rg -n 'example-mod|Example Mod|com\.example|trethore' \
  --glob '!references/**' \
  --glob '!build-logic/**'
```

Review each result rather than blindly replacing it. Documentation may intentionally mention the old values while
explaining the rename.

Run the complete project checks:

```bash
./gradlew check
```

To test the mod in Minecraft afterward:

```bash
./gradlew :packages:fabric-1.21.11:runClient
```

## What can stay unchanged initially

You do not need to rename these classes to get a working mod:

- `Main`
- `FabricBootstrap`
- `ExampleMixin`

Rename `ExampleMixin` once it has a real purpose. When doing so, update its entry in `<mod-id>.mixins.json`.

You also do not need to rename the `common` or `fabric-1.21.11` Gradle modules. Their names describe architecture and
target version rather than your mod's identity.
