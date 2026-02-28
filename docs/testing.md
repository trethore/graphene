# Testing

Graphene has two practical validation layers:

- unit tests under `src/test/java/...`
- in-game debug flows in the debug module

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

These cover bridge serialization and routing behavior, URL/path normalization, HTTP server behavior, MIME detection, viewport/input mapping, and debug selector parsing.

## In-Game Debug Validation

Use the debug client and bundled pages to validate end-to-end behavior:

1. Run `./gradlew runDebugClient`.
2. Press `F10` to open `GrapheneBrowserDebugScreen`.
3. Visit `graphene_test/pages/tests.html` and `graphene_test/pages/automated-tests.html`.
4. Trigger bridge interactions and automated test runs from the page UI.

`automated-tests.html` calls the Java-side debug runner over the bridge (`debug:tests:run`) and renders pass/fail results.

## Commands

Run from repository root:

```bash
./gradlew compileJava
./gradlew test
./gradlew build
./gradlew runDebugClient
./gradlew runDebugClient -PgrapheneDebug=*
./gradlew runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.bridge
```

For logging checks, run one pass without `-PgrapheneDebug` and one with a selector.

## When Adding Features

Keep tests focused and deterministic:

- bridge protocol changes: codec/router/request lifecycle tests
- rendering/input math: mapper/state tests
- lifecycle behavior: navigation/close/pending-request edge cases

For bridge-facing features, prefer both:

1. unit tests for core logic
2. debug-page/manual verification for integration behavior

---

Next: [Overview](overview.md)
