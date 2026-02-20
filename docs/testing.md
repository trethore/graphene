# Testing

Graphene currently has two useful testing layers:

- unit tests (`src/test/java/...`) for protocol and mapping behavior
- in-game debug smoke tests (debug module command)

## Unit Tests In This Repository

Current coverage includes:

- `GrapheneBridgeMessageCodecTest`
  - packet parse behavior
  - payload parse validation
  - success response shape
- `GrapheneBridgeOutboundQueueTest`
  - pre-ready queue buffering
  - flush ordering
  - clear behavior
- `GrapheneClasspathUrlsTest`
  - URL build and normalization semantics
- `GrapheneAppUrlsTest`
  - secure scheme URL build and normalization semantics
- `BrowserSurfaceViewportMapperTest`
  - coordinate scaling/truncation behavior
- `GrapheneDebugLogSelectorTest`
  - wildcard and prefix selector matching
  - comma-separated selector parsing
  - blank/non-matching selector behavior

## In-Game Debug Smoke Tests (This Repository)

The debug module provides `/graphene test`, which runs:

- runtime initialization smoke test
- browser surface smoke test
- bridge API smoke (subscription, emit, and request timeout behavior through public API)
- side mouse bridge smoke for buttons 4..8 (press/release path through `grapheneMouse` bridge)

This is useful before/after larger bridge or lifecycle changes.

Note: when the JS bridge ready handshake is unavailable in a given environment, the mouse smoke test falls back to Java-side input-path validation and logs a warning.

## Commands To Run

Use the following from repository root:

```bash
./gradlew test
./gradlew compileJava
./gradlew build
./gradlew runDebugClient
./gradlew runDebugClient -PgrapheneDebug=*
./gradlew runDebugClient -PgrapheneDebug=tytoo.grapheneui.internal.bridge
```

`runDebugClient` is the fastest way to validate full UI + bridge behavior manually.

For logging verification, run one pass without `-PgrapheneDebug` and one with a package selector to confirm debug output is gated correctly.

## How To Extend Tests

When adding features, prefer small focused tests:

- protocol changes: add/extend codec + router tests first
- sizing/input math: add deterministic mapper/state tests
- lifecycle changes: add queue/pending-request edge case tests

For each new bridge behavior, try to include both:

- unit test for core logic
- debug smoke assertion for end-to-end confirmation

## Suggested Future Test Additions

- `BrowserSurfaceSizingState` edge cases (viewBox clamping and resize mode transitions)
- JS dialog manager queue sequencing
- load listener scope isolation across multiple surfaces
- bridge timeout behavior and cancellation edge cases

---
Next: [Overview](overview.md)
