# Running the Mod

Run the Minecraft client through the Fabric module task:

```sh
./gradlew :packages:<loader>-<version>:runClient
```

To run the dedicated server instead:

```sh
./gradlew :packages:<loader>-<version>:runServer
```

For example for the Fabric 1.21.11 module, that means:

```sh
./gradlew :packages:fabric-1.21.11:runClient
./gradlew :packages:fabric-1.21.11:runServer
```

Each version module uses its own run directories to avoid conflicts between Minecraft versions:

```text
packages/<loader>-<version>/run/client/
packages/<loader>-<version>/run/server/
```

If IntelliJ does not create the Minecraft run configurations automatically after Gradle sync, generate them with:

```sh
./gradlew :packages:fabric-1.21.11:ideaSyncTask
```

## Debug client

Each supported Minecraft version can provide a separate debug-client project. The debug client loads the production
Graphene source set together with a standalone `grapheneui-debug` Fabric mod, while shared static smoke-test pages live
under `debug-client/shared/resources`.

Run the Minecraft 1.21.11 debug client with:

```sh
./gradlew :tools:debug-client-fabric-1.21.11:runDebugClient
```

Press F10 in game to open the browser test screen. Debug classes and resources are not included in the production
Graphene JAR.
