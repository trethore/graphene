# Testing

Graphene has two practical validation layers: unit tests in `common/src/test/java/...` and version modules, plus
in-game debug flows in the version-specific debug module.

**Validation layers**

| Layer        | Command / action                            | Covers                                               |
|--------------|---------------------------------------------|------------------------------------------------------|
| Unit tests   | `./gradlew test`                            | Serialization, URLs, HTTP config, mapping, selectors |
| Build checks | `./gradlew check`                           | Compilation, tests, packaging, common guards         |
| Debug client | `./gradlew :fabric-1.21.11:runDebugClient` | In-game loading, navigation, bridge behavior         |

## Unit Test Coverage

Current test classes include:

- `GrapheneBridgeJsonApiTest`
- `GrapheneBridgeMessageCodecTest`
- `GrapheneBridgeOutboundQueueTest`
- `GrapheneAppUrlsTest`
- `GrapheneClasspathUrlsTest`
- `GrapheneHttpUrlsTest`
- `GrapheneHttpConfigTest`
- `GrapheneConfigTest`
- `GrapheneHttpServerRuntimeTest`
- `GrapheneMimeTypesTest`
- `BrowserSurfaceViewportMapperTest`
- `GrapheneDebugLogSelectorTest`
- `GrapheneLinuxKeyEventPlatformResolverTest`

These cover bridge serialization and routing behavior, URL/path normalization, HTTP server behavior, MIME detection,
viewport/input mapping, and debug selector parsing.

## In-Game Debug Validation

Use the debug client and bundled pages to validate end-to-end behavior:

1. Run `./gradlew :fabric-1.21.11:runDebugClient`.
2. Press `F10` to open `GrapheneBrowserDebugScreen`.
3. Visit `graphene_test/pages/tests.html` and `graphene_test/pages/automated-tests.html`.
4. Trigger bridge interactions and automated test runs from the page UI.

`automated-tests.html` calls the Java-side debug runner over the bridge (`debug:tests:run`) and renders pass/fail
results.

## Commands

Run from repository root:

```bash
./gradlew :common:compileJava :fabric-1.21.11:compileClientJava
./gradlew test
./gradlew check
./gradlew :common:checkNoMinecraftImports
./gradlew :fabric-1.21.11:runDebugClient
./gradlew :fabric-1.21.11:runDebugClient -PgrapheneDebug=*
./gradlew :fabric-1.21.11:runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.bridge
./gradlew :fabric-1.21.11:runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.bridge.GrapheneBridgeRuntime
```

For logging checks, run one pass without `-PgrapheneDebug` and one with a selector. Remove `-PgrapheneDebug` again to
disable Graphene debug logs.

## When Adding Features

Keep tests focused and deterministic:

- bridge protocol changes: codec/router/request lifecycle tests
- rendering/input math: mapper/state tests
- lifecycle behavior: navigation/close/pending-request edge cases

For bridge-facing features, prefer both:

1. unit tests for core logic
2. debug-page/manual verification for integration behavior
